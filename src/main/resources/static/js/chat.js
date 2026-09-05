/**
 * 聊天主页逻辑（独立页面，不再混 Skills 管理）。
 *
 * 依赖：
 *   - common.js 暴露的 window.DA（token / api / auth / theme / toast / renderHeader）
 *   - Vue 3 / marked / DOMPurify / highlight.js 通过 CDN 引入
 *
 * 处理 bubu 原生事件流（PascalCase 类型）：
 *   AgentStart / Thinking / Text / ToolStart / ToolEnd /
 *   TodoProgress / StageOutput / Error / Complete / Paused
 */
const { createApp, ref, nextTick, onMounted, watch } = Vue;

// bubu 事件类型
const DA_AGENTX_EVENTS = {
    AGENT_START: 'AgentStart',
    THINKING: 'Thinking',
    TEXT: 'Text',
    TOOL_START: 'ToolStart',
    TOOL_END: 'ToolEnd',
    TODO_PROGRESS: 'TodoProgress',
    STAGE_OUTPUT: 'StageOutput',
    ERROR: 'Error',
    COMPLETE: 'Complete',
    PAUSED: 'Paused'
};

// todo 文本关键词 → 已完成 tool 映射（用于推断划掉状态）
const TODO_TOOL_MAP = [
    { keys: ['探查', 'schema', '表结构', '列名', '字段'], tools: ['exploreSchema', 'describeTables', 'listTables'] },
    { keys: ['口径', '术语', '活跃', 'VIP', '大额', '指标'], tools: ['lookupGlossary'] },
    { keys: ['图', '可视化', '趋势', '柱状', '折线', '饼', '出图', '画'], tools: ['generate_chart', 'generateChart'] },
    { keys: ['计算', 'python', '环比', '贡献度', '归因'], tools: ['bash'] },
    { keys: ['校验', '验证', '检查'], tools: ['validateSql'] },
    { keys: ['SQL', '查询', '执行', '统计'], tools: ['executeSql'] },
    { keys: ['搜索', '联网', '查一下'], tools: ['tavily_search', 'tavily-search'] },
    { keys: ['加载', '读取', '文档', '文件'], tools: ['analyzeFile'] }
];

createApp({
    setup() {
        // ==================== 状态 ====================
        const messages = ref([]);
        const input = ref('');
        const sending = ref(false);
        const conversationId = ref(getOrCreateConvId());
        const previewUrl = ref('');
        const messagesContainer = ref(null);
        const scroller = ref(null);   // 真正的滚动容器 .da-main
        const textareaRef = ref(null);
        const fileInputRef = ref(null);
        let streamController = null;

        // ==================== 语音输入 ====================
        const isListening = ref(false);
        const voiceSupported = ref(false);
        let recognition = null;
        let finalTranscript = '';

        const initVoice = () => {
            const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
            if (!SR) {
                voiceSupported.value = false;
                return;
            }
            voiceSupported.value = true;
            recognition = new SR();
            recognition.lang = 'zh-CN';
            recognition.continuous = true;
            recognition.interimResults = true;

            recognition.onresult = (event) => {
                let interim = '';
                for (let i = event.resultIndex; i < event.results.length; i++) {
                    const transcript = event.results[i][0].transcript;
                    if (event.results[i].isFinal) {
                        finalTranscript += transcript;
                    } else {
                        interim += transcript;
                    }
                }
                input.value = finalTranscript + interim;
            };

            recognition.onerror = (e) => {
                console.warn('[voice] 识别错误:', e.error);
                isListening.value = false;
            };

            recognition.onend = () => {
                if (isListening.value) {
                    isListening.value = false;
                    const text = input.value.trim();
                    if (text) sendMessage();
                }
            };
        };

        const toggleVoice = () => {
            if (!voiceSupported.value) {
                DA.showToast('当前浏览器不支持语音输入，请使用 Chrome 或 Edge');
                return;
            }
            if (isListening.value) {
                recognition.stop();
            } else {
                finalTranscript = input.value ? input.value : '';
                try {
                    recognition.start();
                    isListening.value = true;
                } catch (e) {
                    console.warn('[voice] 启动失败:', e);
                }
            }
        };

        // 历史会话列表（侧栏展示，分页 + 滚动加载更多）
        const conversations = ref([]);
        const conversationsPage = ref(0);
        const conversationsTotal = ref(0);
        const conversationsHasMore = ref(false);
        const conversationsLoading = ref(false);
        const CONV_PAGE_SIZE = 10;

        // 工具栏状态：联网开关 + 文档上传
        const onlineEnabled = ref(false);
        const uploading = ref(false);
        const uploadedFiles = ref([]);
        const isDragging = ref(false);
        const MAX_FILES = 3;
        const MAX_SIZE_MB = 100;

        // 智能自动滚动：流式追加时只在 autoScroll=true 时滚到底
        const autoScroll = ref(true);
        const SCROLL_BOTTOM_THRESHOLD = 80;

        // 侧栏折叠状态（持久化到 localStorage）
        const sidebarCollapsed = ref(localStorage.getItem('da-sidebar-collapsed') === 'true');
        const toggleSidebar = () => {
            sidebarCollapsed.value = !sidebarCollapsed.value;
            try { localStorage.setItem('da-sidebar-collapsed', String(sidebarCollapsed.value)); } catch (e) { /* ignore */ }
        };

        // 推荐问题
        const suggestions = [
            { text: '你有哪些技能 Skill' },
            { text: '推荐几个关于《火影忍者》的域名' },
            { text: '分析 2005 年员工的销售业绩' },
            { text: '全面分析 2005 年的租赁业务情况' }
        ];

        // ==================== 通用工具 ====================
        const generateId = () => 'da_' + Date.now() + '_' + Math.random().toString(36).slice(2, 9);

        function getOrCreateConvId() {
            const url = new URL(window.location.href);
            return url.searchParams.get('conversationId')
                || ('dodo_conv_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8));
        }

        const scrollToBottom = () => {
            nextTick(() => {
                const el = scroller.value;
                if (!el || !autoScroll.value) return;
                el.scrollTop = el.scrollHeight;
            });
        };

        const onMessagesScroll = () => {
            const el = scroller.value;
            if (!el) return;
            const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
            autoScroll.value = distanceToBottom < SCROLL_BOTTOM_THRESHOLD;
        };

        const scrollToBottomManual = () => {
            autoScroll.value = true;
            nextTick(() => {
                const el = scroller.value;
                if (el) el.scrollTop = el.scrollHeight;
            });
        };

        const previewImage = (url) => { previewUrl.value = url; };

        // ==================== Markdown ====================
        const setupMarkdown = () => {
            if (typeof DOMPurify !== 'undefined' && !DOMPurify.__daLinkHookAdded) {
                DOMPurify.addHook('afterSanitizeAttributes', (node) => {
                    if (node && node.nodeName === 'A') {
                        node.setAttribute('target', '_blank');
                        node.setAttribute('rel', 'noopener noreferrer nofollow');
                    }
                });
                DOMPurify.__daLinkHookAdded = true;
            }
            if (typeof marked !== 'undefined' && typeof hljs !== 'undefined') {
                marked.setOptions({
                    highlight(code, lang) {
                        if (lang && hljs.getLanguage(lang)) {
                            try { return hljs.highlight(code, { language: lang }).value; } catch (e) {}
                        }
                        return hljs.highlightAuto(code).value;
                    },
                    breaks: true,
                    gfm: true,
                    sanitize: false
                });
            }
        };

        const renderMarkdown = (content) => {
            if (!content) return '';
            if (typeof marked === 'undefined') return content;
            const processed = content
                .replace(/\\n/g, '\n')
                .replace(/\\r\\n/g, '\n')
                .replace(/\\r/g, '\n');
            const html = marked.parse(processed);
            return typeof DOMPurify !== 'undefined' ? DOMPurify.sanitize(html) : html;
        };

        // ==================== 时间线 / Todo 辅助 ====================
        const dotClass = (item) => {
            if (item.type === 'thinking') return 'thinking';
            if (item.type === 'text') return 'text';
            if (item.type === 'error') return 'error';
            if (item.type === 'todo') return 'todo';
            return item.status || 'running';
        };

        const todoIcon = (status) => {
            if (status === 'completed') return 'fas fa-check-circle';
            if (status === 'in_progress') return 'fas fa-circle-half-stroke';
            return 'far fa-circle';
        };

        const completedCount = (items) =>
            (items || []).filter(t => t.status === 'completed').length;

        // ==================== 工具结果解析 ====================
        const safeJsonParse = (s, fallback) => {
            try { return JSON.parse(s); } catch (e) { return fallback; }
        };

        const formatArguments = (argStr) => {
            if (!argStr) return '';
            const obj = safeJsonParse(argStr, null);
            if (obj && typeof obj === 'object') return JSON.stringify(obj, null, 2);
            return argStr;
        };

        // 解析 generate_chart 返回文本里的图片 URL
        const parseChartUrl = (text) => {
            if (!text) return null;
            const urlMatch = text.match(/https?:\/\/[^\s)'"]+\.(?:png|jpg|jpeg|gif|webp)/i);
            if (urlMatch) {
                const titleMatch = text.match(/title\s*[:：]\s*([^\n]+)/i);
                return { url: urlMatch[0], title: titleMatch ? titleMatch[1].trim() : '图表' };
            }
            const md = text.match(/!\[([^\]]*)\]\(([^)]+)\)/);
            if (md) return { url: md[2], title: md[1] || '图表' };
            return null;
        };

        // 基于已完成的 tool 推断 todo 状态
        const inferTodoStatus = (todo, completedTools) => {
            if (todo.status === 'completed') return 'completed';
            const text = (todo.text || todo.content || todo.task || '').toLowerCase();
            for (const mapping of TODO_TOOL_MAP) {
                if (mapping.keys.some(k => text.includes(k.toLowerCase()))) {
                    if (mapping.tools.some(t => completedTools.has(t))) {
                        return 'completed';
                    }
                }
            }
            return todo.status || 'pending';
        };

        // ==================== 消息构建 ====================
        const newAssistantMsg = () => {
            const msg = {
                id: generateId(),
                role: 'assistant',
                content: '',
                timeline: [],
                charts: [],
                loading: true,
                timestamp: Date.now()
            };
            messages.value.push(msg);
            return messages.value[messages.value.length - 1];
        };

        // ==================== 事件分发 ====================
        const buildTodoSnapshot = (items, aiMsg) => {
            const completedTools = new Set(
                aiMsg.timeline
                    .filter(it => it.type === 'tool' && it.status === 'completed')
                    .map(it => it.toolName)
            );
            return items.map(it => {
                const todo = {
                    text: it.text || it.content || it.task || '',
                    status: it.status || 'pending'
                };
                if (todo.status !== 'completed') {
                    todo.status = inferTodoStatus(todo, completedTools);
                }
                return todo;
            });
        };

        const dispatchToolResult = (toolName, resultText, aiMsg) => {
            const lname = (toolName || '').toLowerCase();
            if (lname === 'generate_chart' || lname === 'generatechart') {
                const chart = parseChartUrl(resultText);
                if (chart) aiMsg.charts.push(chart);
            }
        };

        const dispatchStageOutput = (stage, data, aiMsg) => {
            const payload = (data && typeof data === 'object' && data.content !== undefined)
                ? data.content : data;
            switch (stage) {
                case 'chart_image': {
                    const obj = typeof payload === 'string' ? safeJsonParse(payload, {}) : (payload || {});
                    if (obj.url) {
                        aiMsg.charts.push({ url: obj.url, title: obj.title || '图表', type: obj.type || '' });
                    }
                    break;
                }
                case 'report':
                    aiMsg.report = typeof payload === 'string' ? payload : (payload.content || '');
                    break;
                case 'recommend': {
                    const arr = typeof payload === 'string' ? safeJsonParse(payload, []) : (payload || []);
                    if (Array.isArray(arr)) {
                        aiMsg.recommend = arr.map(x =>
                            typeof x === 'string' ? x : (x.question || x.text || '')
                        );
                    }
                    break;
                }
                default:
                    console.debug('[chat] 忽略 stage', stage);
            }
        };

        const processAgentxEvent = (event, aiMsg) => {
            switch (event.type) {
                case DA_AGENTX_EVENTS.AGENT_START:
                    aiMsg.loading = true;
                    break;

                case DA_AGENTX_EVENTS.THINKING: {
                    const content = event.content || '';
                    const last = aiMsg.timeline[aiMsg.timeline.length - 1];
                    if (last && last.type === 'thinking') {
                        last.content += content;
                    } else {
                        aiMsg.timeline.push({ type: 'thinking', content });
                    }
                    break;
                }

                case DA_AGENTX_EVENTS.TEXT: {
                    const content = event.content || '';
                    if (!content) break;
                    const last = aiMsg.timeline[aiMsg.timeline.length - 1];
                    if (last && last.type === 'text') {
                        last.content += content;
                    } else {
                        aiMsg.timeline.push({ type: 'text', content });
                    }
                    break;
                }

                case DA_AGENTX_EVENTS.TOOL_START: {
                    // TodoWrite 走独立 todo 面板，不进 tool 卡片
                    if ((event.toolName || '').toLowerCase() === 'todowrite') {
                        const args = safeJsonParse(event.arguments || '', {});
                        const todos = Array.isArray(args.todos) ? args.todos : [];
                        if (todos.length) {
                            aiMsg.timeline.push({
                                type: 'todo',
                                items: buildTodoSnapshot(todos, aiMsg)
                            });
                        }
                        break;
                    }
                    aiMsg.timeline.push({
                        type: 'tool',
                        toolName: event.toolName || 'unknown',
                        toolCallId: event.toolCallId || '',
                        arguments: event.arguments || '',
                        status: 'running',
                        result: '',
                        showResult: false
                    });
                    break;
                }

                case DA_AGENTX_EVENTS.TOOL_END: {
                    if ((event.toolName || '').toLowerCase() === 'todowrite') break;
                    const toolCallId = event.toolCallId || '';
                    const entry = aiMsg.timeline.find(it =>
                        it.type === 'tool' && it.toolCallId === toolCallId && it.status === 'running'
                    );
                    if (entry) {
                        entry.status = 'completed';
                        entry.result = event.result || '';
                    } else {
                        aiMsg.timeline.push({
                            type: 'tool',
                            toolName: event.toolName || 'unknown',
                            toolCallId,
                            arguments: '',
                            status: 'completed',
                            result: event.result || '',
                            showResult: false
                        });
                    }
                    dispatchToolResult(event.toolName, event.result || '', aiMsg);
                    break;
                }

                case DA_AGENTX_EVENTS.TODO_PROGRESS: {
                    const items = Array.isArray(event.items) ? event.items : [];
                    aiMsg.timeline.push({
                        type: 'todo',
                        items: buildTodoSnapshot(items, aiMsg)
                    });
                    break;
                }

                case DA_AGENTX_EVENTS.STAGE_OUTPUT:
                    dispatchStageOutput(event.stage, event.data, aiMsg);
                    break;

                case DA_AGENTX_EVENTS.ERROR:
                    aiMsg.timeline.push({
                        type: 'error',
                        message: event.message || '未知错误',
                        detail: event.detail || ''
                    });
                    break;

                case DA_AGENTX_EVENTS.COMPLETE:
                    aiMsg.loading = false;
                    break;

                case DA_AGENTX_EVENTS.PAUSED:
                    break;

                default:
                    console.debug('[chat] 未知事件类型', event);
            }
        };

        // ==================== 发送消息（SSE） ====================
        const sendMessage = async (presetText) => {
            const text = (presetText !== undefined ? presetText : input.value).toString().trim();
            if (!text || sending.value) return;

            const successFiles = uploadedFiles.value
                .filter(f => f.status === 'SUCCESS' && f.fileId)
                .map(f => ({
                    fileId: f.fileId,
                    fileName: f.fileName,
                    fileType: f.fileType,
                    fileSize: f.fileSize,
                    previewUrl: f.previewUrl || ''
                }));

            messages.value.push({
                id: generateId(),
                role: 'user',
                content: text,
                attachments: successFiles,
                timestamp: Date.now()
            });

            if (presetText === undefined) input.value = '';
            if (textareaRef.value) textareaRef.value.style.height = 'auto';

            const aiMsg = newAssistantMsg();
            autoScroll.value = true;
            scrollToBottom();
            sending.value = true;

            uploadedFiles.value = [];
            uploading.value = false;

            // 单 ReactAgent 统一入口：POST /agent/stream
            const fileIds = successFiles.map(f => f.fileId);
            const requestBody = {
                query: text,
                conversationId: conversationId.value
            };
            if (onlineEnabled.value) {
                requestBody.online = true;
            }
            if (fileIds.length > 0) {
                requestBody.fileIds = fileIds;
            }

            streamController = DA.apiStream('/agent/stream', requestBody, {
                onEvent(eventData, eventName) {
                    // bubu 的事件 data 是 JSON 字符串
                    let event = eventData;
                    if (typeof eventData === 'string') {
                        event = safeJsonParse(eventData, null);
                        if (!event) {
                            // 不是 JSON，按 event name 构造简单事件
                            event = { type: eventName, content: eventData };
                        }
                    }
                    if (event && event.type) {
                        processAgentxEvent(event, aiMsg);
                        scrollToBottom();
                    }
                },
                onError(err) {
                    console.error('[chat] 流式请求失败', err);
                    aiMsg.timeline.push({
                        type: 'error',
                        message: '请求失败',
                        detail: err.message || ''
                    });
                    aiMsg.loading = false;
                    sending.value = false;
                    streamController = null;
                },
                onClose() {
                    aiMsg.loading = false;
                    sending.value = false;
                    streamController = null;
                    // 框架已自动持久化 timeline，刷新侧栏历史列表
                    loadConversations();
                    scrollToBottom();
                }
            });
        };

        const stopMessage = async () => {
            if (!sending.value) return;
            try {
                await DA.apiGet('/agent/stop', { conversationId: conversationId.value });
            } catch (e) { /* ignore */ }
            if (streamController) {
                streamController.abort();
                streamController = null;
            }
            sending.value = false;
            const lastAi = [...messages.value].reverse().find(m => m.role === 'assistant');
            if (lastAi) lastAi.loading = false;
        };

        const newConversation = () => {
            if (sending.value) {
                if (streamController) streamController.abort();
                sending.value = false;
            }
            // 释放本地 object URL
            messages.value.forEach(m => {
                if (m.attachments) m.attachments.forEach(a => {
                    if (a.previewUrl) { try { URL.revokeObjectURL(a.previewUrl); } catch (e) { /* ignore */ } }
                });
            });
            uploadedFiles.value.forEach(f => {
                if (f.previewUrl) { try { URL.revokeObjectURL(f.previewUrl); } catch (e) { /* ignore */ } }
            });
            messages.value = [];
            uploadedFiles.value = [];
            uploading.value = false;
            const newCid = 'dodo_conv_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8);
            conversationId.value = newCid;
            const u = new URL(window.location.href);
            u.searchParams.set('conversationId', newCid);
            window.history.replaceState({}, '', u.toString());
        };

        // ==================== 历史会话 ====================
        const loadConversations = async (opts) => {
            const reset = !opts || opts.reset;
            const page = reset ? 0 : conversationsPage.value + 1;
            if (conversationsLoading.value) return;
            if (!reset && !conversationsHasMore.value) return;
            conversationsLoading.value = true;
            try {
                const body = await DA.apiGet('/session/list', { page, size: CONV_PAGE_SIZE });
                const p = body && body.data ? body.data : null;
                const items = p && Array.isArray(p.conversations) ? p.conversations : [];
                if (reset) {
                    conversations.value = items;
                } else {
                    const existIds = new Set(conversations.value.map(c => c.conversationId));
                    conversations.value = conversations.value.concat(
                        items.filter(c => !existIds.has(c.conversationId))
                    );
                }
                conversationsPage.value = p ? p.page : page;
                conversationsTotal.value = p ? p.total : items.length;
                conversationsHasMore.value = p ? p.hasMore : false;
            } catch (e) {
                console.warn('[chat] 加载历史会话失败', e);
            } finally {
                conversationsLoading.value = false;
            }
        };

        const onConvListScroll = (e) => {
            const el = e && e.target;
            if (!el || conversationsLoading.value || !conversationsHasMore.value) return;
            const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
            if (distanceToBottom < 40) {
                loadConversations({ reset: false });
            }
        };

        const loadConversation = async (convId) => {
            if (sending.value) return;
            try {
                const body = await DA.apiGet('/session/' + encodeURIComponent(convId));
                if (!body.data || !body.data.messages) return;

                const loaded = [];
                for (const round of body.data.messages) {
                    loaded.push({
                        id: 'hist_u_' + round.id,
                        role: 'user',
                        content: round.question,
                        attachments: (round.attachments || []).map(a => ({
                            fileId: a.fileId,
                            fileName: a.fileName,
                            fileType: a.fileType,
                            fileSize: a.fileSize,
                            previewUrl: /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test('.' + (a.fileType || ''))
                                ? `${DA.BACKEND_URL}/files/${a.fileId}/preview`
                                : ''
                        })),
                        timestamp: Date.now()
                    });

                    // 框架原生 timeline：TimelineEntry[] 纯数组
                    let items = [];
                    if (round.timeline) {
                        const parsed = safeJsonParse(round.timeline, null);
                        items = Array.isArray(parsed) ? parsed : [];
                    }
                    items = items.map(it =>
                        it.type === 'tool' ? { ...it, showResult: false } : it
                    );
                    if (items.length === 0 && round.answer) {
                        items.push({ type: 'text', content: round.answer });
                    }

                    // 从工具结果中重建 charts
                    const charts = [];
                    for (const it of items) {
                        if (it.type === 'tool' && it.result) {
                            const lname = (it.toolName || '').toLowerCase();
                            if (lname === 'generate_chart' || lname === 'generatechart') {
                                const chart = parseChartUrl(it.result);
                                if (chart) charts.push(chart);
                            }
                        }
                    }

                    loaded.push({
                        id: 'hist_a_' + round.id,
                        role: 'assistant',
                        content: round.answer || '',
                        timeline: items,
                        charts,
                        loading: false,
                        timestamp: Date.now()
                    });
                }

                messages.value = loaded;
                conversationId.value = convId;
                const u = new URL(window.location.href);
                u.searchParams.set('conversationId', convId);
                window.history.replaceState({}, '', u.toString());
                nextTick(() => scrollToBottom());
            } catch (e) {
                console.error('[chat] 加载会话失败', e);
            }
        };

        const deleteConversation = async (convId) => {
            if (!confirm('确认删除该会话？')) return;
            try {
                await DA.apiDelete('/session/' + encodeURIComponent(convId));
                DA.showToast('已删除');
                if (convId === conversationId.value) {
                    newConversation();
                }
                await loadConversations();
            } catch (e) {
                console.error('[chat] 删除会话失败', e);
            }
        };

        const formatConvTime = (isoStr) => {
            if (!isoStr) return '';
            try {
                const d = new Date(isoStr);
                const diffMs = Date.now() - d.getTime();
                const diffMin = Math.floor(diffMs / 60000);
                const diffHour = Math.floor(diffMs / 3600000);
                const diffDay = Math.floor(diffMs / 86400000);
                if (diffMin < 1) return '刚刚';
                if (diffMin < 60) return diffMin + ' 分钟前';
                if (diffHour < 24) return diffHour + ' 小时前';
                if (diffDay < 7) return diffDay + ' 天前';
                return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
            } catch (e) {
                return '';
            }
        };

        // ==================== 文档上传 ====================
        const triggerUpload = () => {
            if (fileInputRef.value) fileInputRef.value.click();
        };

        const onFileSelected = (e) => {
            const files = Array.from(e.target.files || []);
            files.forEach(uploadFile);
            if (fileInputRef.value) fileInputRef.value.value = '';
        };

        const IMAGE_EXT_RE = /^(jpg|jpeg|png|gif|bmp|webp)$/i;
        const isImageFile = (f) => {
            const ext = (f && f.fileType) ? f.fileType.toLowerCase() : '';
            return IMAGE_EXT_RE.test(ext);
        };
        const fileThumb = (f) => f.previewUrl || f.minioUrl || '';

        const uploadFile = async (file) => {
            if (uploadedFiles.value.length >= MAX_FILES) {
                DA.showToast(`最多上传 ${MAX_FILES} 个文件`);
                return;
            }
            if (file.size > MAX_SIZE_MB * 1024 * 1024) {
                DA.showToast(`文件 "${file.name}" 超过 ${MAX_SIZE_MB}MB`);
                return;
            }

            const lowerName = file.name.toLowerCase();
            const isImg = /\.(jpg|jpeg|png|gif|bmp|webp)$/.test(lowerName);
            const placeholder = {
                fileId: null,
                fileName: file.name,
                fileType: lowerName.split('.').pop() || '',
                fileSize: file.size,
                status: 'uploading',
                previewUrl: isImg ? URL.createObjectURL(file) : '',
                minioUrl: ''
            };
            uploadedFiles.value.push(placeholder);
            uploading.value = true;

            try {
                const formData = new FormData();
                formData.append('file', file);
                const json = await DA.apiUpload('/api/files/upload', formData);
                if (!json.data) throw new Error(json.msg || '上传失败');
                placeholder.fileId = json.data.fileId;
                placeholder.fileName = json.data.fileName;
                placeholder.fileType = json.data.fileType;
                placeholder.fileSize = json.data.fileSize;
                placeholder.status = json.data.status || 'SUCCESS';
            } catch (e) {
                console.error('[chat] 上传失败', e);
                placeholder.status = 'FAILED';
                DA.showToast(`"${file.name}" 上传失败: ${e.message}`);
            } finally {
                uploading.value = uploadedFiles.value.some(f => f.status === 'uploading');
            }
        };

        const removeFile = async (idx) => {
            const f = uploadedFiles.value[idx];
            if (!f) return;
            if (f.fileId) {
                try {
                    await DA.apiDelete('/api/files/' + f.fileId);
                } catch (e) {
                    console.warn('[chat] 删除文件元数据失败', e);
                }
            }
            if (f.previewUrl) {
                try { URL.revokeObjectURL(f.previewUrl); } catch (e) { /* ignore */ }
            }
            uploadedFiles.value.splice(idx, 1);
            uploading.value = uploadedFiles.value.some(f => f.status === 'uploading');
        };

        const onFileDrop = (e) => {
            e.preventDefault();
            isDragging.value = false;
            const files = Array.from(e.dataTransfer.files || []);
            files.forEach(uploadFile);
        };
        const onFileDragOver = (e) => {
            e.preventDefault();
            isDragging.value = true;
        };
        const onFileDragLeave = (e) => {
            e.preventDefault();
            isDragging.value = false;
        };

        // ==================== 顶栏 ====================
        // 由 common.js 的 renderHeader 渲染，本组件只需调用绑定函数
        const currentUser = ref(null);

        // ==================== 生命周期 ====================
        onMounted(async () => {
            // 鉴权守卫：未登录跳 /login.html
            const user = await DA.requireAuth();
            if (!user) return;
            currentUser.value = user;

            setupMarkdown();
            initVoice();

            // 渲染顶栏（用 innerHTML 注入 common.js 的 renderHeader）
            const headerHost = document.getElementById('da-header-host');
            if (headerHost) {
                headerHost.innerHTML = DA.renderHeader({
                    active: 'chat',
                    user: user,
                    sidebarToggle: true
                });
                DA.bindHeaderEvents();
                // 侧栏折叠按钮（仅聊天页有）
                const sidebarToggleBtn = document.getElementById('da-sidebar-toggle');
                if (sidebarToggleBtn) {
                    sidebarToggleBtn.addEventListener('click', () => toggleSidebar());
                }
            }

            const u = new URL(window.location.href);
            const urlConvId = u.searchParams.get('conversationId');
            u.searchParams.set('conversationId', conversationId.value);
            window.history.replaceState({}, '', u.toString());

            loadConversations();
            if (urlConvId && urlConvId === conversationId.value) {
                loadConversation(urlConvId);
            }
        });

        watch(input, () => {
            nextTick(() => {
                if (textareaRef.value) {
                    textareaRef.value.style.height = 'auto';
                    textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px';
                }
            });
        });

        return {
            messages,
            input,
            sending,
            conversationId,
            previewUrl,
            suggestions,
            conversations,
            conversationsTotal,
            conversationsHasMore,
            conversationsLoading,
            onConvListScroll,
            onlineEnabled,
            uploading,
            uploadedFiles,
            isDragging,
            sidebarCollapsed,
            toggleSidebar,
            messagesContainer,
            scroller,
            textareaRef,
            fileInputRef,
            autoScroll,
            sendMessage,
            stopMessage,
            toggleVoice,
            isListening,
            voiceSupported,
            newConversation,
            scrollToBottomManual,
            onMessagesScroll,
            renderMarkdown,
            previewImage,
            formatArguments,
            triggerUpload,
            onFileSelected,
            onFileDrop,
            onFileDragOver,
            onFileDragLeave,
            removeFile,
            isImageFile,
            fileThumb,
            loadConversation,
            deleteConversation,
            formatConvTime,
            dotClass,
            todoIcon,
            completedCount
        };
    }
}).mount('#da-app');
