const { createApp, ref, computed, onMounted, onUnmounted } = Vue;

createApp({
    setup() {
        const documents = ref([]);
        const loading = ref(false);
        const uploading = ref(false);
        const dragging = ref(false);
        const keyword = ref('');
        const status = ref('');
        const page = ref(1);
        const size = 20;
        const total = ref(0);
        const pages = computed(() => Math.max(1, Math.ceil(total.value / size)));
        let pollTimer = null;

        const loadDocuments = async () => {
            loading.value = true;
            try {
                const body = await DA.apiGet('/v1/knowledge-documents', {
                    page: page.value, size, keyword: keyword.value, status: status.value
                });
                const data = body.data || {};
                documents.value = data.records || [];
                total.value = Number(data.total || 0);
                updatePolling();
            } catch (error) {
                DA.showToast(error.message || '文档加载失败');
            } finally {
                loading.value = false;
            }
        };

        const upload = async file => {
            if (!file || uploading.value) return;
            uploading.value = true;
            try {
                const form = new FormData();
                form.append('file', file);
                const body = await DA.apiUpload('/v1/knowledge-documents/upload', form);
                DA.showToast('文档已提交处理');
                page.value = 1;
                await loadDocuments();
                if (body.data && body.data.documentId) openDocument(body.data.documentId);
            } catch (error) {
                const existing = error.response && error.response.data;
                if (error.response && error.response.code === 40901 && existing) {
                    DA.showToast('相同文档已经上传');
                    openDocument(existing.documentId);
                } else {
                    DA.showToast(error.message || '上传失败');
                }
            } finally {
                uploading.value = false;
            }
        };

        const selectFile = event => {
            upload(event.target.files && event.target.files[0]);
            event.target.value = '';
        };
        const dropFile = event => {
            dragging.value = false;
            upload(event.dataTransfer.files && event.dataTransfer.files[0]);
        };
        const search = () => { page.value = 1; loadDocuments(); };
        const previousPage = () => { if (page.value > 1) { page.value--; loadDocuments(); } };
        const nextPage = () => { if (page.value < pages.value) { page.value++; loadDocuments(); } };
        const openDocument = id => { window.location.href = 'document-detail.html?documentId=' + encodeURIComponent(String(id)); };
        const formatDate = value => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-';
        const processing = value => ['uploaded', 'parsing', 'splitting', 'embedding'].includes(value);
        const updatePolling = () => {
            const shouldPoll = documents.value.some(doc => processing(doc.documentStatus));
            if (shouldPoll && !pollTimer) pollTimer = setInterval(loadDocuments, 5000);
            if (!shouldPoll && pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        };
        const statusClass = value => 'is-' + (value || 'unknown');
        const statusIcon = value => processing(value) ? 'fas fa-circle-notch fa-spin' : value === 'vectored' ? 'fas fa-circle-check' : value === 'failed' ? 'fas fa-triangle-exclamation' : 'fas fa-circle';
        const fileIcon = type => type && type.includes('pdf') ? 'far fa-file-pdf' : type && (type.includes('word') || type.includes('document')) ? 'far fa-file-word' : 'far fa-file-lines';

        onMounted(async () => {
            const user = await DA.requireAuth();
            if (!user) return;
            document.getElementById('da-header-host').innerHTML = DA.renderHeader({ active: 'documents', user });
            DA.bindHeaderEvents();
            loadDocuments();
        });
        onUnmounted(() => { if (pollTimer) clearInterval(pollTimer); });

        return { documents, loading, uploading, dragging, keyword, status, page, total, pages,
            loadDocuments, selectFile, dropFile, search, previousPage, nextPage, openDocument,
            formatDate, statusClass, statusIcon, fileIcon };
    }
}).mount('#da-documents-app');
