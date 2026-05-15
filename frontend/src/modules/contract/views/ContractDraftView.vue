<template>
  <div class="page-container draft-container fade-in">
    <!-- Header Controls -->
    <div class="draft-header mb-6">
      <div class="header-left">
        <div class="title-wrapper">
          <input 
            v-model="contractForm.name" 
            class="title-input" 
            placeholder="请输入合同名称"
          />

        </div>
      </div>
      <div class="header-right">
        <button class="btn btn-secondary mr-3" @click="toggleSidebar">
          <span class="icon"></span> {{ aiSidebarVisible ? '收拢助手' : '呼出助手' }}
        </button>
        <button class="btn btn-secondary mr-3" @click="saveDraft" :disabled="isSavingDraft">
          <span class="icon"></span> {{ isSavingDraft ? '保存中...' : '存草稿' }}
        </button>
        <button class="btn btn-secondary mr-3" @click="submitForReview" :disabled="!contractForm.content.trim() || isSavingDraft">
          <span class="icon"></span> {{ isSavingDraft ? '提交中...' : '提交审查' }}
        </button>
        <button class="btn btn-primary" @click="generateContract" :disabled="isGenerating">
          <span class="icon">✨</span> {{ isGenerating ? '生成中...' : '生成合同' }}
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
              <label>合同金额 (元)</label>
              <input type="number" class="form-input font-mono" v-model="contractForm.amount" />
            </div>
            <div class="form-group">
              <label>合同类型</label>
              <select class="form-input" v-model="contractForm.type">
                <option
                  v-for="option in CONTRACT_TYPE_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
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
                复制全文
              </button>
              <button class="btn-action text-xs" @click="downloadContent" v-if="contractForm.content">
                下载TXT
              </button>
              <span class="text-xs text-muted">支持双向同步与 AI 补写</span>
            </div>
          </div>
          <div class="doc-body pt-3 flex-1 flex flex-col">
            <AiThinkingPanel
              v-if="isGenerating"
              class="mb-3"
              title="AI 起草合同中"
              subtitle="腾讯混元 + 得理法规 RAG + 本地条款知识库"
              :steps="[
                '解析合同元信息与核心需求',
                '调用得理法规检索匹配上位法',
                '本地条款知识库 TF-IDF 召回',
                '混元大模型起草正文',
                '回写合同标题与摘要'
              ]"
              :step-interval-ms="1800"
            />
            <textarea
              v-else
              class="doc-textarea font-serif flex-1"
              v-model="contractForm.content"
              placeholder="在此开始撰写合同正文，或使用右侧 AI 助手依据模板结构自动生成条款..."
            ></textarea>
            <div v-if="!isGenerating && lastDraftMeta" class="draft-meta mt-3">
              <ConfidenceBadge :value="lastDraftMeta.confidence" />
              <span class="draft-meta-note">起草已参考：法规 {{ draftRagCounts.law }} 条 · 知识库 {{ draftRagCounts.kb }} 段</span>
              <button class="btn-link text-xs" @click="showDraftRag = !showDraftRag">
                {{ showDraftRag ? '收起检索来源' : '展开检索来源' }}
              </button>
            </div>
            <div v-if="!isGenerating && showDraftRag && lastDraftMeta" class="draft-rag-grid mt-3">
              <RagSourceList
                v-if="(lastDraftMeta.retrievalContext?.laws?.length ?? 0) > 0"
                kind="law"
                chip-prefix="L"
                title-prefix="得理 · 法律法规"
                :items="lastDraftMeta.retrievalContext?.laws ?? []"
              />
              <RagSourceList
                v-if="(lastDraftMeta.retrievalContext?.knowledge?.length ?? 0) > 0"
                kind="kb"
                chip-prefix="K"
                title-prefix="本地 RAG 知识库"
                :items="lastDraftMeta.retrievalContext?.knowledge ?? []"
              />
            </div>
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
            <div v-if="msg.type === 'bot'" class="msg-content">{{ msg.content }}</div>
            <div v-else class="msg-content-user">{{ msg.content }}</div>
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
import { computed, reactive, ref, nextTick, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { submitConsultation, submitContractDraft } from '@/shared/api/legal';
import { createContract, getContract, updateContract } from '@/shared/api/contracts';
import { CONTRACT_TYPE_OPTIONS } from '@/shared/constants/contractTypes';
import { toast } from '@/shared/ui/toast';
import type { ContractItem } from '@/shared/types/contracts';
import type { ContractDraftResponse } from '@/shared/types/legal';
import AiThinkingPanel from '@/shared/ui/AiThinkingPanel.vue';
import ConfidenceBadge from '@/shared/ui/ConfidenceBadge.vue';
import RagSourceList from '@/shared/ui/RagSourceList.vue';

const route = useRoute();
const router = useRouter();
const chatFlowRef = ref<HTMLElement>();

const aiSidebarVisible = ref(true);
const aiMode = ref<'ASK' | 'AGENT'>('ASK');
const isGenerating = ref(false);
const isSavingDraft = ref(false);
const isAiProcessing = ref(false);
const aiInput = ref('');
const currentDraftId = ref<number | null>(null);
const linkedContract = ref<ContractItem | null>(null);

interface ChatMessage {
  type: 'user' | 'bot';
  content: string;
}

const chatMessages = ref<ChatMessage[]>([]);
const lastDraftMeta = ref<ContractDraftResponse | null>(null);
const showDraftRag = ref(false);

const draftRagCounts = computed(() => ({
  law: lastDraftMeta.value?.retrievalContext?.laws?.length ?? 0,
  kb: lastDraftMeta.value?.retrievalContext?.knowledge?.length ?? 0,
}));

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

function detectChatIntent(text: string): 'identity' | 'capability' | 'greeting' | 'thanks' | null {
  const t = text.toLowerCase().replace(/[\s。！？!?,，.]/g, '');
  if (/(你是谁|你叫什么|你的名字|你是什么|你是个什么|是谁啊|介绍一下你|介绍你自己|whoareyou|whatsyourname|whatareyou)/.test(t)) return 'identity';
  if (/(你能做什么|你能干什么|你有什么功能|你会什么|你有什么能力|有哪些功能|whatcanyoudo|有什么用)/.test(t)) return 'capability';
  if (/^(你好|您好|hi|hello|嗨|哈喽|在吗|hey)$/.test(t)) return 'greeting';
  if (/^(谢谢|多谢|thx|thanks|感谢)/.test(t)) return 'thanks';
  return null;
}

const CHAT_PRESETS: Record<string, string> = {
  identity:
    '我是 LexAI 法务专属 AI —— 基于腾讯混元 (Hunyuan) + 得理法律检索 API + 本地法规/合同知识库构建的法律辅助助手。\n\n在合同起草页，我可以帮你：\n• 解读现行条款的法律风险\n• 提炼对方诉求要素\n• 切到「Agent 修改指令」模式直接改写左侧正文\n\n你也可以直接问我任何法律或合规问题，我会调用法律检索 + LLM 给出带引用的回答。',
  capability:
    '我能做这些事：\n1. 法律咨询：实时检索得理法律法规 + 本地知识库，再由混元 LLM 给出带 [L#]/[K#] 引用的答案\n2. 合同风险审查：缺失项、违约金合理性、双方义务失衡、保密期限…\n3. Agent 模式：根据你的自然语言指令直接改正文（如「把违约金从 3% 提到 5%」）\n4. 自动给出建议 / 风险提示 / 法律依据，并附自评可信度\n\n切换右上角「Agent 修改指令」即可让我直接改左侧正文。',
  greeting: '你好，我是 LexAI 法务 AI。可以问我条款相关的法律问题，或者切到「Agent 修改指令」模式让我直接改正文。',
  thanks: '不客气～有其他法律或合同问题随时问我。',
};

const sendAiMessage = async () => {
  if (!aiInput.value.trim()) return;

  const userMessage = aiInput.value.trim();
  aiInput.value = '';

  chatMessages.value.push({
    type: 'user',
    content: userMessage
  });

  await scrollChatToBottom();

  if (aiMode.value === 'ASK') {
    const intent = detectChatIntent(userMessage);
    if (intent && CHAT_PRESETS[intent]) {
      chatMessages.value.push({ type: 'bot', content: CHAT_PRESETS[intent] });
      await scrollChatToBottom();
      return;
    }
  }

  isAiProcessing.value = true;

  try {
    if (aiMode.value === 'ASK') {
      const facts: string[] = [];
      if (contractForm.name?.trim()) facts.push(`合同名称：${contractForm.name.trim()}`);
      if (contractForm.type?.trim()) facts.push(`合同类型：${contractForm.type.trim()}`);
      if (contractForm.partyA?.trim()) facts.push(`甲方：${contractForm.partyA.trim()}`);
      if (contractForm.partyB?.trim()) facts.push(`乙方：${contractForm.partyB.trim()}`);
      if (Number(contractForm.amount) > 0) facts.push(`金额：${contractForm.amount} 元`);
      if (contractForm.duration?.trim()) facts.push(`期限：${contractForm.duration.trim()}`);
      if (contractForm.content?.trim()) {
        facts.push(`当前合同正文（节选）：${contractForm.content.slice(0, 600)}`);
      }

      const resp = await submitConsultation({
        question: userMessage,
        facts,
        createFollowUpTask: false
      });

      const parts: string[] = [];
      const stripCitations = (s: string) => s.replace(/\[(L|C|K|N\/A)\d*\]/gi, '').trim();
      if (resp.answer && resp.answer.trim()) {
        parts.push(stripCitations(resp.answer));
      }
      if (resp.recommendations?.length) {
        parts.push(`✅ 建议\n- ${resp.recommendations.map(stripCitations).join('\n- ')}`);
      }
      if (resp.riskAlerts?.length) {
        parts.push(`⚠️ 风险提示\n- ${resp.riskAlerts.map(stripCitations).join('\n- ')}`);
      }
      if (resp.legalBasis?.length) {
        parts.push(`📚 法律依据\n- ${resp.legalBasis.slice(0, 3).map(stripCitations).join('\n- ')}`);
      }

      chatMessages.value.push({
        type: 'bot',
        content: parts.length
          ? parts.join('\n\n')
          : `（识别领域：${resp.category || '综合咨询'}）抱歉，本次未能命中具体法律依据。可以补充更多事实细节后再问，或在「Agent 修改指令」模式直接让我改正文。`
      });
    } else {
      if (!contractForm.content.trim()) {
        toast('请先生成合同正文，再使用 Agent 修改。', 'warning');
        chatMessages.value.push({
          type: 'bot',
          content: '请先点击“生成合同”生成正文，然后我再根据指令修改。'
        });
        return;
      }

      const mergedRequirements = [
        contractForm.requirements?.trim() || '',
        `修改需求：${userMessage}`,
        '当前合同全文：',
        contractForm.content
      ].filter(Boolean).join('\n\n');

      const resp = await submitContractDraft({
        contractName: contractForm.name,
        contractType: contractForm.type,
        partyA: contractForm.partyA,
        partyB: contractForm.partyB,
        amount: Math.round(Number(contractForm.amount) || 0),
        duration: contractForm.duration,
        requirements: mergedRequirements,
        createFollowUpTask: false
      });

      contractForm.content = resp.generatedContent;
      chatMessages.value.push({
        type: 'bot',
        content: '✅ 已根据你的指令完成修改，并已自动替换左侧正文。'
      });
    }
  } catch (error: any) {
    console.error('AI 助手调用失败:', error);
    chatMessages.value.push({
      type: 'bot',
      content: `调用失败：${error?.message || '后端服务暂不可用，请稍后再试。'}`
    });
  } finally {
    isAiProcessing.value = false;
    scrollChatToBottom();
  }
};

function validateDraftInputs(): string | null {
  if (!contractForm.name.trim()) return '请输入合同名称';
  if (!contractForm.partyA.trim()) return '请输入甲方名称';
  if (!contractForm.partyB.trim()) return '请输入乙方名称';
  const amount = Number(contractForm.amount);
  if (!Number.isFinite(amount) || amount <= 0) return '请输入有效的合同金额';
  return null;
}

function validateDraftSave(): string | null {
  if (!contractForm.name.trim()) return '请先输入合同名称';
  if (!contractForm.content.trim() && !contractForm.partyA.trim() && !contractForm.partyB.trim()) {
    return '请至少补充合同正文或一项基础信息后再保存';
  }
  return null;
}

const generateContract = async () => {
  const err = validateDraftInputs();
  if (err) {
    toast(err, 'warning');
    return;
  }

  isGenerating.value = true;
  try {
    const response = await submitContractDraft({
      contractName: contractForm.name,
      contractType: contractForm.type,
      partyA: contractForm.partyA,
      partyB: contractForm.partyB,
      amount: Math.round(Number(contractForm.amount) || 0),
      duration: contractForm.duration,
      requirements: contractForm.requirements,
      createFollowUpTask: false
    });
    contractForm.content = response.generatedContent;
    lastDraftMeta.value = response;
    showDraftRag.value = false;
    toast('合同已生成', 'success');
  } finally {
    isGenerating.value = false;
  }
};

async function persistContract(status: 'DRAFT' | 'UNDER_REVIEW') {
  const payload = {
    name: contractForm.name.trim(),
    contractType: contractForm.type,
    partyA: contractForm.partyA.trim(),
    partyB: contractForm.partyB.trim(),
    amount: Number(contractForm.amount) || 0,
    content: contractForm.content,
    source: 'AI_DRAFT',
    status
  };
  const wasExistingDraft = currentDraftId.value !== null;
  const saved = wasExistingDraft
    ? await updateContract(currentDraftId.value!, payload)
    : await createContract(payload);
  currentDraftId.value = saved.id;
  linkedContract.value = saved;
  return saved;
}

async function loadExistingDraft(contractId: number) {
  const contract = await getContract(contractId);
  linkedContract.value = contract;
  currentDraftId.value = contract.id;
  contractForm.name = contract.name;
  contractForm.type = contract.contractType;
  contractForm.partyA = contract.partyA;
  contractForm.partyB = contract.partyB;
  contractForm.amount = Number(contract.amount) || 0;
  contractForm.content = contract.content || '';
}

const saveDraft = async () => {
  const err = validateDraftSave();
  if (err) {
    toast(err, 'warning');
    return;
  }

  isSavingDraft.value = true;
  try {
    const saved = await persistContract('DRAFT');
    toast(`草稿已保存：${saved.contractNo}`, 'success');
  } catch (error: any) {
    console.error('保存草稿失败:', error);
    toast(`保存草稿失败：${error?.message || '请稍后重试'}`, 'error');
  } finally {
    isSavingDraft.value = false;
  }
};

const submitForReview = async () => {
  if (!contractForm.content.trim()) {
    toast('请先生成或填写合同正文', 'warning');
    return;
  }
  if (!contractForm.name.trim()) {
    toast('请先填写合同名称', 'warning');
    return;
  }
  if (linkedContract.value && linkedContract.value.status !== 'DRAFT' && linkedContract.value.status !== 'UNDER_REVIEW') {
    toast(`当前合同状态为「${linkedContract.value.status}」，不能再次提交审查。请先在合同台账重置状态或新建合同。`, 'warning');
    return;
  }

  isSavingDraft.value = true;
  try {
    const saved = await persistContract('UNDER_REVIEW');
    toast(`已提交审查：${saved.contractNo}`, 'success');
    router.push({
      name: 'contractReview',
      query: { contractId: String(saved.id) }
    });
  } catch (error: any) {
    console.error('提交审查失败:', error);
    toast(`提交审查失败：${error?.message || '请稍后重试'}`, 'error');
  } finally {
    isSavingDraft.value = false;
  }
};

const copyContent = async () => {
  try {
    await navigator.clipboard.writeText(contractForm.content);
    toast('已复制到剪贴板', 'success');
  } catch (error) {
    toast('复制失败，请重试', 'error');
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

onMounted(async () => {
  const rawContractId = route.query.contractId;
  const contractId = typeof rawContractId === 'string' ? Number(rawContractId) : Number.NaN;
  if (!Number.isInteger(contractId) || contractId <= 0) {
    return;
  }

  try {
    await loadExistingDraft(contractId);
  } catch (error) {
    console.error('加载合同草稿失败:', error);
    toast('加载合同草稿失败，请稍后重试', 'error');
  }
});
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
  grid-template-columns: minmax(0, 1fr) 400px;
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

.draft-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  font-size: 0.78rem;
  color: var(--text-muted);
}
.draft-meta-note { color: var(--text-muted); }
.btn-link {
  background: transparent;
  border: none;
  color: var(--primary);
  cursor: pointer;
  text-decoration: underline;
  font-size: 0.75rem;
}
.btn-link:hover { opacity: 0.8; }

.draft-rag-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 0.75rem;
}
.draft-rag-grid > * {
  min-width: 0;
}
@media (max-width: 1024px) {
  .draft-rag-grid { grid-template-columns: 1fr; }
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
