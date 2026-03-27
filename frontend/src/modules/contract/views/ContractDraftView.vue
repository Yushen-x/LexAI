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
        <button class="btn btn-primary" @click="submitForReview" :disabled="isSubmitting">
          <span class="icon">✨</span> {{ isSubmitting ? '生成中...' : '生成并提交审查' }}
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
                <option value="采购合同">标准采购合同</option>
                <option value="技术服务">技术服务框架协议</option>
                <option value="保密协议">保密协议 (NDA)</option>
                <option value="劳动合同">劳动合同</option>
                <option value="租赁合同">租赁合同</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Document Editor Card -->
        <div class="card doc-card flex-1 flex flex-col">
          <div class="card-header border-b pb-3 flex justify-between items-center">
            <h3 class="card-title text-sm">正文实时撰写区</h3>
            <div class="flex gap-2">
              <button class="btn-action text-xs" @click="copyContent" v-if="contractForm.content">
                📋 复制全文
              </button>
              <button class="btn-action text-xs" @click="downloadContent" v-if="contractForm.content">
                ⬇️ 下载TXT
              </button>
              <span class="text-xs text-muted">支持双向同步与 AI 补写</span>
            </div>
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
        
        <div class="chat-flow padding-box flex-1" ref="chatFlowRef">
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

          <!-- Generated messages will appear here -->
          <div v-for="(msg, idx) in chatMessages" :key="idx" class="chat-bubble" :class="msg.type === 'user' ? 'user-msg' : 'bot-msg'">
            <div v-if="msg.type === 'bot'" class="msg-avatar">Lex</div>
            <div class="msg-content">
              {{ msg.content }}
              <div v-if="msg.agentAction" class="agent-preview mt-2">
                <div class="preview-tag text-xs">{{ msg.agentAction.actionType }} 操作</div>
                <div class="preview-text text-sm">{{ msg.agentAction.content }}</div>
                <div class="flex justify-end mt-2">
                  <span class="text-primary text-xs cursor-pointer hover-underline" @click="applyAgentAction(idx)">
                    应用此修改
                  </span>
                  <span class="text-muted text-xs cursor-pointer hover-underline ml-3" @click="cancelAgentAction(idx)">
                    撤销
                  </span>
                </div>
              </div>
            </div>
            <div v-if="msg.type === 'user'" class="msg-content-user">
              {{ msg.content }}
            </div>
          </div>

          <!-- Loading indicator -->
          <div v-if="isAiProcessing" class="chat-bubble bot-msg">
            <div class="msg-avatar">Lex</div>
            <div class="msg-content">
              <div class="loading-dots">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <div class="ai-input-area border-t padding-box">
          <textarea 
            v-model="aiInput"
            class="chat-input" 
            rows="2" 
            :placeholder="aiMode === 'ASK' ? '询问法律或流程问题...' : '输入修改正文的指令 (例如：把违约金提升到 5%)'"
            @keydown.enter.ctrl="sendAiMessage"
          ></textarea>
          <div class="flex justify-end mt-2">
            <button class="btn btn-primary text-sm px-4 py-1" @click="sendAiMessage" :disabled="!aiInput.trim() || isAiProcessing">
              {{ isAiProcessing ? '处理中...' : '发送指令' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { submitContractDraft } from '@/shared/api/legal';

const router = useRouter();
const chatFlowRef = ref<HTMLElement>();

const aiSidebarVisible = ref(true);
const aiMode = ref<'ASK' | 'AGENT'>('ASK');
const isGenerating = ref(false);
const isSubmitting = ref(false);
const isAiProcessing = ref(false);
const aiInput = ref('');

interface ChatMessage {
  type: 'user' | 'bot';
  content: string;
  agentAction?: {
    actionType: string;
    content: string;
  };
}

const chatMessages = ref<ChatMessage[]>([]);

const contractForm = reactive({
  name: '未命名云服务项目合同',
  type: '采购合同',
  partyA: '中国移动通信集团浙江有限公司',
  partyB: '',
  amount: 500000,
  duration: '12个月',
  content: '',
  requirements: ''
});

const toggleSidebar = () => {
  aiSidebarVisible.value = !aiSidebarVisible.value;
};

const scrollChatToBottom = async () => {
  await nextTick();
  if (chatFlowRef.value) {
    chatFlowRef.value.scrollTop = chatFlowRef.value.scrollHeight;
  }
};

const sendAiMessage = async () => {
  if (!aiInput.value.trim()) return;

  const userMessage = aiInput.value.trim();
  aiInput.value = '';

  chatMessages.value.push({
    type: 'user',
    content: userMessage
  });

  isAiProcessing.value = true;
  await scrollChatToBottom();

  setTimeout(() => {
    if (aiMode.value === 'ASK') {
      const response = generateAskResponse(userMessage);
      chatMessages.value.push({
        type: 'bot',
        content: response
      });
    } else {
      const agentResponse = generateAgentResponse(userMessage);
      chatMessages.value.push({
        type: 'bot',
        content: agentResponse.message,
        agentAction: {
          actionType: 'INSERT',
          content: agentResponse.suggestedText
        }
      });
    }
    isAiProcessing.value = false;
    scrollChatToBottom();
  }, 800);
};

const generateAskResponse = (question: string): string => {
  // 基于关键词提供专业建议（模板回复，用于兜底）
  const keywords = question.toLowerCase();
  
  if (keywords.includes('风险')) {
    return '合同主要风险点：\n1. 付款条件不明确 → 建议明确付款期限、方式、发票要求\n2. 违约责任含糊 → 建议量化违约金比例或赔偿标准\n3. 交付/验收标准不清 → 建议附加具体收货验收规则\n4. 争议解决机制缺失 → 建议增加仲裁或诉讼条款\n5. 保密责任不对等 → 建议明确保密范围和期限\n\n您想重点讨论哪个？';
  }
  if (keywords.includes('条款')) {
    return '标准合同条款结构建议（按法律规范）：\n① 合同主体与标的 - 明确双方主体、服务内容或货物\n② 权利义务条款 - 详细列举双方的权利与义务\n③ 付款与交付 - 支付节点、金额、交付方式、验收标准\n④ 违约责任 - 违约金比例、赔偿范围、解除权\n⑤ 争议解决 - 适用法律、管辖法院/仲裁机构\n⑥ 其他条款 - 保密、知识产权、不可抗力等\n\n需要我为您补充某个具体条款吗？';
  }
  if (keywords.includes('保密')) {
    return '保密条款应包含以下要素：\n✓ 保密信息定义 - 明确哪些属于商业秘密\n✓ 保密范围 - 双方均受约束还是单向保密\n✓ 保密期限 - 建议5-10年（根据行业标准）\n✓ 排除情况 - 公知信息、法定披露、独立开发\n✓ 违反后果 - 违约金、赔偿责任、追究责任\n✓ 救济手段 - 可申请禁令或要求赔偿\n\n需要我为您补充保密条款吗？';
  }
  
  return '我已阅读您的提问。可以帮助您：\n→ 分析合同风险点并提出防范方案\n→ 完善合同条款结构和法律描述\n→ 优化具体条款内容（付款、交付、违约等）\n→ 调整双方权利义务的平衡性\n\n请具体告诉我您想改进哪个方面！';
};

const generateAgentResponse = (instruction: string) => {
  // 根据用户指令生成标准法律条款建议（模板回复）
  const lowerInstruction = instruction.toLowerCase();
  
  if (lowerInstruction.includes('违约金') || lowerInstruction.includes('违约') || lowerInstruction.includes('责任')) {
    return {
      message: '✅ 已为您生成标准违约责任条款，请核对后使用。',
      suggestedText: `第六条  违约责任
6.1 甲方违约：如甲方未按合同规定完成履约义务或交付标准不达要求，应按逾期天数向乙方支付违约金（建议按日0.1%-0.5%计算），逾期超过30天乙方有权单方面解除合同并要求赔偿损失。
6.2 乙方违约：如乙方未按时支付合同款项，应按逾期天数支付相应违约金；如违反保密或其他重要义务，应承担相应法律责任。
6.3 赔偿责任：任何一方因过错或违约给对方造成的直接经济损失，应在违约金之外足额赔偿。
6.4 解除权：一方重大违约经通知后30日内仍未改正的，对方有权发出解除通知函，合同立即解除。`
    };
  }
  
  if (lowerInstruction.includes('保密') || lowerInstruction.includes('机密') || lowerInstruction.includes('知识产权')) {
    return {
      message: '✅ 已为您补充保密与知识产权条款，请核对内容。',
      suggestedText: `第五条  保密与知识产权
5.1 保密定义：双方对履行本合同过程中获知的商业秘密、技术方案、客户信息、财务数据等信息承诺严格保密。
5.2 保密期限：保密义务在本合同终止后继续有效，期限为5年（如法律规定更长期限则按法律规定）。
5.3 保密例外：根据法律强制要求必须披露、公知信息、独立开发信息、第三方已知信息除外。
5.4 知识产权权属：本合同履行过程中产生的知识产权（包括专利、著作权、商业秘密等）的归属权和使用权按双方另行商定协议确定。
5.5 违反后果：任何一方违反保密义务给对方造成损失的，应承担赔偿责任，特别严重情形下对方有权申请禁令。`
    };
  }

  if (lowerInstruction.includes('付款') || lowerInstruction.includes('支付') || lowerInstruction.includes('价格')) {
    return {
      message: '✅ 已为您生成详细的付款条款，请确认修改。',
      suggestedText: `第三条  费用与支付
3.1 合同总价：人民币 ${contractForm.amount} 元整（大写：${Math.round(contractForm.amount / 10000)}万元，详见发票）
3.2 支付安排：合同总价分阶段支付
  - 签订合同时：支付总价的30%（¥${Math.round(contractForm.amount * 0.3)}元）
  - 服务/商品交付时：支付总价的40%（¥${Math.round(contractForm.amount * 0.4)}元）
  - 验收通过/使用满意期后：支付剩余30%（¥${Math.round(contractForm.amount * 0.3)}元）
3.3 支付方式：银行转账汇款至乙方指定账户，以银行凭证为支付凭证
3.4 发票：乙方应在收款后10日内按金额开具增值税发票
3.5 逾期利息：甲方逾期支付，按日利率 0.05‰ 计算逾期利息并计入应付款项`
    };
  }

  if (lowerInstruction.includes('交付') || lowerInstruction.includes('验收') || lowerInstruction.includes('交货')) {
    return {
      message: '✅ 已为您补充交付与验收条款，请核对。',
      suggestedText: `第四条  交付与验收
4.1 交付时间：${contractForm.duration || '12个月'}内完成交付（具体时间表详见附件）
4.2 交付地点：乙方指定地点（含运输、安装等）或远程交付（根据商品性质）
4.3 交付标准：交付的商品/服务应满足
  - 符合国家相关法律法规和行业标准
  - 符合合同约定的规格、数量、质量要求
  - 包装完整，标识清晰
4.4 验收流程：乙方收到交付物后 7 天内进行验收
  - 验收合格：乙方签收确认，视为接受
  - 有瑕疵：乙方通知甲方，甲方在 5 天内修复或重新交付
  - 不符合要求：乙方有权拒收并要求退款
4.5 风险转移：交付物交付乙方、乙方签收后，风险责任转移至乙方`
    };
  }

  return {
    message: '📝 已收到您的修改指令，转化为标准法律条款。',
    suggestedText: `【指令处理】"${instruction}"\n\n✓ 该修改建议已转化为符合法律标准的条款模板\n✓ 点击下方"应用修改"按钮将其添加到左侧编辑区\n✓ 您可以在编辑区进一步调整细节，或继续输入其他修改指令\n\n提示：每条指令可以反复调整，直到满足为止。`
  };
};

const applyAgentAction = (messageIdx: number) => {
  const msg = chatMessages.value[messageIdx];
  if (msg.agentAction) {
    contractForm.content += '\n\n' + msg.agentAction.content;
    chatMessages.value.push({
      type: 'bot',
      content: '✅ 修改已应用！您可以在左侧编辑区查看更新。'
    });
    scrollChatToBottom();
  }
};

const cancelAgentAction = (messageIdx: number) => {
  chatMessages.value.splice(messageIdx, 1);
};

const submitForReview = async () => {
  if (!contractForm.name) {
    alert('请输入合同名称');
    return;
  }

  isSubmitting.value = true;

  try {
    // 调用后端生成合同
    const response = await submitContractDraft({
      contractName: contractForm.name,
      contractType: contractForm.type,
      partyA: contractForm.partyA,
      partyB: contractForm.partyB,
      amount: contractForm.amount,
      duration: contractForm.duration,
      requirements: contractForm.requirements
    });

    // 将生成的合同内容填入编辑区
    contractForm.content = response.generatedContent;

    // 存储到sessionStorage用于审查页面
    sessionStorage.setItem('pendingContractContent', response.generatedContent);
    sessionStorage.setItem('pendingContractName', contractForm.name);

    // 跳转到审查页面
    router.push({
      name: 'ContractReview',
      query: {
        fromDraft: 'true'
      }
    });
  } catch (error) {
    console.error('生成合同失败:', error);
    alert('生成合同失败，请稍后重试或检查网络连接');
  } finally {
    isSubmitting.value = false;
  }
};

const copyContent = async () => {
  try {
    await navigator.clipboard.writeText(contractForm.content);
    alert('已复制到剪贴板');
  } catch (error) {
    alert('复制失败，请重试');
  }
};

const downloadContent = () => {
  const element = document.createElement('a');
  element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(contractForm.content));
  element.setAttribute('download', `${contractForm.name}.txt`);
  element.style.display = 'none';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
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

.gap-2 { gap: 0.5rem; }
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

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

.btn-action {
  background: transparent;
  border: none;
  color: var(--primary);
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-action:hover {
  opacity: 0.8;
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
  animation: chatSlideIn 0.3s ease-out;
}

@keyframes chatSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

.msg-content-user {
  background: var(--primary);
  color: white;
  padding: 0.875rem 1rem;
  border-radius: var(--radius-lg) 0 var(--radius-lg) var(--radius-lg);
  font-size: 0.875rem;
  line-height: 1.5;
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
  white-space: pre-wrap;
  word-break: break-word;
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

.chat-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-dots span {
  display: inline-block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--text-muted);
  animation: loadingDots 1.4s infinite;
  margin: 0 2px;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes loadingDots {
  0%, 60%, 100% {
    opacity: 0.6;
  }
  30% {
    opacity: 1;
  }
}
</style>
