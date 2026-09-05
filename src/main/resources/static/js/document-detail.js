const { createApp, ref, computed, onMounted, onUnmounted } = Vue;

createApp({
    setup() {
        const documentId = new URLSearchParams(window.location.search).get('documentId');
        const documentInfo = ref(null);
        const segments = ref([]);
        const loading = ref(false);
        const page = ref(1);
        const size = 50;
        const total = ref(0);
        const pages = computed(() => Math.max(1, Math.ceil(total.value / size)));
        const editing = ref(null);
        const editContent = ref('');
        const saving = ref(false);
        const versionModal = ref(false);
        const versionFile = ref(null);
        const versionUploading = ref(false);
        const increment = ref('PATCH');
        let pollTimer = null;

        const loadDocument = async () => {
            const body = await DA.apiGet('/v1/knowledge-documents/' + encodeURIComponent(documentId));
            documentInfo.value = body.data;
        };
        const loadSegments = async () => {
            const body = await DA.apiGet('/v1/knowledge-documents/' + encodeURIComponent(documentId) + '/segments', { page: page.value, size });
            segments.value = (body.data && body.data.records) || [];
            total.value = Number((body.data && body.data.total) || 0);
        };
        const refresh = async () => {
            if (!documentId) { window.location.replace('documents.html'); return; }
            loading.value = true;
            try {
                await Promise.all([loadDocument(), loadSegments()]);
                updatePolling();
            } catch (error) {
                DA.showToast(error.message || '文档详情加载失败');
            } finally {
                loading.value = false;
            }
        };

        const processing = value => ['uploaded', 'parsing', 'splitting', 'embedding'].includes(value);
        const updatePolling = () => {
            const shouldPoll = (documentInfo.value && processing(documentInfo.value.documentStatus))
                || segments.value.some(item => item.status === 'embedding');
            if (shouldPoll && !pollTimer) pollTimer = setInterval(refresh, 5000);
            if (!shouldPoll && pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        };
        const statusClass = value => 'is-' + (value || 'unknown');
        const statusIcon = value => processing(value) ? 'fas fa-circle-notch fa-spin' : value === 'vectored' ? 'fas fa-circle-check' : value === 'failed' ? 'fas fa-triangle-exclamation' : 'fas fa-circle';
        const formatDate = value => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-';
        const isBusy = segment => segment.status === 'embedding' || documentInfo.value.documentStatus === 'embedding';
        const canVectorize = segment => segment.status === 'saved' || segment.status === 'failed';

        const openEdit = segment => { editing.value = segment; editContent.value = segment.segmentContent; };
        const closeEdit = () => { if (!saving.value) { editing.value = null; editContent.value = ''; } };
        const nextPatchVersion = computed(() => {
            const value = documentInfo.value && documentInfo.value.version;
            if (!value) return 'PATCH';
            const parts = value.split('.').map(Number);
            return parts.length === 3 ? `${parts[0]}.${parts[1]}.${parts[2] + 1}` : 'PATCH';
        });
        const saveEdit = async () => {
            if (!editing.value || !editContent.value.trim() || saving.value) return;
            saving.value = true;
            try {
                const body = await DA.apiPut('/v1/knowledge-documents/' + encodeURIComponent(documentId)
                    + '/segments/' + encodeURIComponent(editing.value.chunkId), {
                    segmentContent: editContent.value,
                    lockVersion: editing.value.lockVersion,
                    baseVersion: documentInfo.value.version
                });
                DA.showToast('已创建版本 ' + body.data.version);
                closeEdit();
                await refresh();
            } catch (error) {
                DA.showToast(error.status === 409 ? '内容已变化，请刷新后重试' : (error.message || '保存失败'));
            } finally {
                saving.value = false;
                if (editing.value && !saving.value) closeEdit();
            }
        };

        const vectorize = async segment => {
            if (!canVectorize(segment)) return;
            segment.status = 'embedding';
            segment.statusName = '向量化中';
            try {
                await DA.apiPost('/v1/knowledge-documents/' + encodeURIComponent(documentId)
                    + '/segments/' + encodeURIComponent(segment.chunkId) + '/vectorize', {
                    lockVersion: segment.lockVersion
                });
                updatePolling();
            } catch (error) {
                DA.showToast(error.message || '向量化提交失败');
                await loadSegments();
            }
        };

        const openVersionModal = () => { versionModal.value = true; increment.value = 'PATCH'; versionFile.value = null; };
        const closeVersionModal = () => { if (!versionUploading.value) versionModal.value = false; };
        const uploadVersion = async () => {
            if (!versionFile.value || versionUploading.value) return;
            versionUploading.value = true;
            const form = new FormData();
            form.append('file', versionFile.value);
            const query = '?baseVersion=' + encodeURIComponent(documentInfo.value.version)
                + '&increment=' + encodeURIComponent(increment.value);
            try {
                const body = await DA.apiUpload('/v1/knowledge-documents/' + encodeURIComponent(documentId) + '/versions' + query, form);
                DA.showToast('已提交版本 ' + body.data.version);
                versionModal.value = false;
                await refresh();
            } catch (error) {
                DA.showToast(error.message || '新版本上传失败');
            } finally {
                versionUploading.value = false;
            }
        };

        const changePage = delta => {
            const target = page.value + delta;
            if (target >= 1 && target <= pages.value) { page.value = target; loadSegments(); }
        };

        onMounted(async () => {
            const user = await DA.requireAuth();
            if (!user) return;
            window.document.getElementById('da-header-host').innerHTML = DA.renderHeader({ active: 'documents', user });
            DA.bindHeaderEvents();
            refresh();
        });
        onUnmounted(() => { if (pollTimer) clearInterval(pollTimer); });

        return { document: documentInfo, segments, loading, page, total, pages, editing, editContent, saving,
            versionModal, versionFile, versionUploading, increment, nextPatchVersion,
            refresh, formatDate, statusClass, statusIcon, isBusy, canVectorize, openEdit,
            closeEdit, saveEdit, vectorize, openVersionModal, closeVersionModal, uploadVersion,
            changePage };
    }
}).mount('#da-document-detail-app');
