<template>
  <div class="page-container draft-container fade-in">
    <!-- Header Controls -->
    <div class="draft-header mb-6">
      <div class="header-left">
        <button class="btn btn-icon mr-4" @click="$router.back()" title="返回">
          <span class="icon">🔙</span>
        </button>
        <div class="title-wrapper">
          <input 
            v-model="contractForm.name" 
            class="title-input" 
            placeholder="请输入合同名称"
          />
          <span class="badge badge-warning ml-3">合同智能起草模式</span>
        </div>
      </div>
      <div class="header-right">
        <button class="btn btn-secondary mr-3" @click="toggleSidebar">
          <span class="icon">🤖</span> {{ aiSidebarVisible ? '收拢助手' : '呼出助手' }}
        </button>
        <button class="btn btn-secondary mr-3">存草稿</button>
        <button class="btn btn-primary">
          <span class="icon">✨</span> 生成并提交审查
        </button>
      </div>
    </div>

    <!-- Main Workspace -->
    <div class="workspace-grid" :class="{ 'sidebar-open': aiSidebarVisible }">
      <!-- Editor Panel -->
      <div class="editor-col flex-col flex gap-4">
        <!-- Meta Info Card -->
        <div class="card meta-card">
          <div class="card-header border-b pb-3">
            <h3 class="card-title text-sm">合同基础元信息</h3>
          </div>
          <div class="meta-grid pt-4">
            <div class="form-group">
              <label>甲方名称</label>
              <input type="text" class="form-input" v-model="contractForm.partyA" />
            </div>
            <div class="form-group">
              <label>乙方名称</label>
              <input type="text" class="form-input" v-model="contractForm.partyB" placeholder="输入合作方名称..." />
            </div>
            <div class="form-group">
              <label>涉案标的额 (元)</label>
              <input type="number" class="form-input font-mono" v-model="contractForm.amount" />
            </div>
            <div class="form-group">
              <label>合同类型</label>
              <select class="form-input" v-model="contractForm.type">
                <option value="TYPE_A">标准采购采购合同</option>
                <option value="TYPE_B">技术服务框架协议</option>
                <option value="TYPE_C">保密协议 (NDA)</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Document Editor Card -->
        <div class="card doc-card flex-1 flex flex-col">
          <div class="card-header border-b pb-3 flex justify-between items-center">
            <h3 class="card-title text-sm">正文实时撰写区</h3>
            <span class="text-xs text-muted">支持双向同步与 AI 补写</span>
          </div>
          <div class="doc-body pt-3 flex-1 flex">
            <textarea 
              class="doc-textarea font-serif" 
              v-model="contractForm.content" 
              placeholder="在此开始撰写合同正文，或使用右侧 AI 助手依据模板结构自动生成条款..."
            ></textarea>
          </div>
        </div>
      </div>

      <!-- AI Assistant Sidebar -->
      <div class="ai-col card flex-col" v-show="aiSidebarVisible">
        <div class="ai-header padding-box border-b">
          <div class="flex items-center justify-between mb-3">
            <h3 class="card-title text-primary"><span class="icon mr-2">🤖</span>法务专属 AI</h3>
            <button class="btn-close" @click="toggleSidebar">×</button>
          </div>
          <div class="mode-tabs">
            <button class="tab-btn" :class="{ active: aiMode === 'ASK' }" @click="aiMode = 'ASK'">问答咨询</button>
            <button class="tab-btn" :class="{ active: aiMode === 'AGENT' }" @click="aiMode = 'AGENT'">Agent 修改指令</button>
          </div>
        </div>
        
        <div class="chat-flow padding-box flex-1">
          <!-- Initial Welcome Message -->
          <div class="chat-bubble bot-msg">
            <div class="msg-avatar">Lex</div>
            <div class="msg-content">
              你好，我是 LexAI 核心助手。你可以：<br><br>
              1. 让我帮你查控条款风险<br>
              2. 让我帮你提炼对方的诉求要素<br>
              3. 切换到 <b>Agent 模式</b>，让我直接对左侧正文进行段落改写或补充。
            </div>
          </div>
          
          <!-- Sample User Message -->
          <div class="chat-bubble user-msg">
            <div class="msg-content">
              帮我在左侧基于刚才的标的额，生成一份标准的违约责任条款。
            </div>
          </div>

          <!-- Sample Agent Action -->
          <div class="chat-bubble bot-msg">
            <div class="msg-avatar">Lex</div>
            <div class="msg-content">
              已接受指令。我编写了符合标的额量级的违约责任，请核对。
              <div class="agent-preview mt-2">
                <div class="preview-tag text-xs">INSERT 操作</div>
                <div class="preview-text text-sm">第七条 违约责任：若乙方逾期交付，每日需按照合同总额的 0.1% 支付违约金...</div>
                <div class="flex justify-end mt-2">
                  <span class="text-primary text-xs cursor-pointer hover-underline">撤销此修改</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="ai-input-area border-t padding-box">
          <textarea 
            class="chat-input" 
            rows="2" 
            :placeholder="aiMode === 'ASK' ? '询问法律或流程问题...' : '输入修改正文的指令 (例如：把违约金提升到 5%)'"
          ></textarea>
          <div class="flex justify-end mt-2">
            <button class="btn btn-primary text-sm px-4 py-1">发送指令</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';

const aiSidebarVisible = ref(true);
const aiMode = ref<'ASK' | 'AGENT'>('ASK');

const contractForm = reactive({
  name: '未命名云服务项目合同',
  type: 'TYPE_A',
  partyA: '中国移动通信集团浙江有限公司',
  partyB: '',
  amount: 500000,
  content: ''
});

const toggleSidebar = () => {
  aiSidebarVisible.value = !aiSidebarVisible.value;
};
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
  height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
}

.fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.mb-6 { margin-bottom: 1.5rem; }
.mr-2 { margin-right: 0.5rem; }
.mr-3 { margin-right: 0.75rem; }
.mr-4 { margin-right: 1rem; }
.ml-3 { margin-left: 0.75rem; }
.mt-2 { margin-top: 0.5rem; }
.pb-3 { padding-bottom: 0.75rem; }
.pt-4 { padding-top: 1rem; }
.pt-3 { padding-top: 0.75rem; }
.px-4 { padding-left: 1rem; padding-right: 1rem; }
.py-1 { padding-top: 0.25rem; padding-bottom: 0.25rem; }

.border-b { border-bottom: 1px solid var(--border-light); }
.border-t { border-top: 1px solid var(--border-light); }

.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.text-muted { color: var(--text-muted); }
.text-primary { color: var(--primary); }
.font-mono { font-family: monospace; }
.font-serif { font-family: "Noto Serif SC", serif, Arial, sans-serif; }

.gap-4 { gap: 1rem; }
.flex { display: flex; }
.flex-1 { flex: 1; }
.flex-col { flex-direction: column; }
.justify-between { justify-content: space-between; }
.justify-end { justify-content: flex-end; }
.items-center { align-items: center; }

.cursor-pointer { cursor: pointer; }
.hover-underline:hover { text-decoration: underline; }

/* Header */
.draft-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-surface);
  padding: 1.25rem 2rem;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  border: none !important;
}

.header-left {
  display: flex;
  align-items: center;
}

.title-wrapper {
  display: flex;
  align-items: center;
}

.title-input {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-strong);
  border: none;
  background: transparent;
  outline: none;
  width: 280px;
  border-bottom: 2px solid transparent;
  transition: border-color 0.2s;
  padding-bottom: 0.25rem;
}

.title-input:focus {
  border-bottom-color: var(--primary);
}

.title-input::placeholder {
  color: var(--text-muted);
  font-weight: 400;
}

/* Workspace Grid */
.workspace-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
  flex: 1;
  min-height: 0;
  transition: all 0.3s ease;
}

.workspace-grid.sidebar-open {
  grid-template-columns: 1fr 400px;
}

.editor-col {
  min-width: 0; /* flex/grid text overflow fix */
}

/* Forms & Doc Editor */
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

.form-group label {
  display: block;
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-bottom: 0.5rem;
}

.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-app);
  color: var(--text-main);
  outline: none;
  transition: border-color 0.2s;
}

.form-input:focus {
  border-color: var(--primary);
}

.doc-card {
  margin-bottom: 0;
}

.doc-textarea {
  width: 100%;
  resize: none;
  border: none;
  background: var(--bg-app);
  border-radius: var(--radius-md);
  padding: 1.5rem;
  color: var(--text-strong);
  font-size: 1rem;
  line-height: 1.8;
  outline: none;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.02);
}

/* AI Sidebar */
.ai-col {
  display: flex;
  background: var(--bg-surface);
  border: none !important;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.padding-box {
  padding: 1.25rem;
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  color: var(--text-muted);
  cursor: pointer;
  line-height: 1;
}
.btn-close:hover { color: var(--text-strong); }

.mode-tabs {
  display: flex;
  background: var(--bg-app);
  border-radius: var(--radius-md);
  padding: 0.25rem;
}

.tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 0.5rem;
  font-size: 0.875rem;
  color: var(--text-muted);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: white;
  color: var(--primary);
  box-shadow: var(--shadow-sm);
  font-weight: 500;
}

/* Chat Flow */
.chat-flow {
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: var(--bg-app);
}

.chat-bubble {
  display: flex;
  gap: 0.75rem;
  max-width: 90%;
}

.msg-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1e3a8a, #3b82f6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: bold;
}

.msg-content {
  background: white;
  padding: 0.875rem 1rem;
  border-radius: 0 var(--radius-lg) var(--radius-lg) var(--radius-lg);
  font-size: 0.875rem;
  color: var(--text-main);
  line-height: 1.5;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.user-msg {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.user-msg .msg-content {
  background: var(--primary);
  color: white;
  border-radius: var(--radius-lg) 0 var(--radius-lg) var(--radius-lg);
}

.agent-preview {
  margin-top: 0.75rem;
  background: var(--bg-app);
  border: 1px dashed var(--border-light);
  padding: 0.75rem;
  border-radius: var(--radius-md);
}

.preview-tag {
  color: #15803d;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.preview-text {
  color: var(--text-muted);
}

/* Chat Input */
.chat-input {
  width: 100%;
  resize: none;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  font-size: 0.875rem;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input:focus {
  border-color: var(--primary);
}
</style>
