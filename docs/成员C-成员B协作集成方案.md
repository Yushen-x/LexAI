# 成员C与成员B协作方案 - 合同存储集成

> 当前状态：成员B完成数据库后的集成步骤

---

## 当前状况

### 现有实现
- 成员C："存草稿"函数使用 `localStorage` 保存到浏览器本地
- 问题：页面关闭、清除缓存或切换设备后数据丢失

### 目标
- 改为调用成员B的后端接口 `POST /api/contracts`
- 实现真正的持久化存储
- 在合同台账中显示已存储的草稿

---

## 实现方案

### 第1步：确认成员B的接口契约

**接口地址：** `POST /api/contracts`  
**请求体：**
```json
{
  "name": "合同名称",           // 对应 contractForm.name
  "type": "采购合同",            // 对应 contractForm.type
  "partyA": "甲方",              // 对应 contractForm.partyA
  "partyB": "乙方",              // 对应 contractForm.partyB
  "amount": 500000,              // 对应 contractForm.amount
  "content": "合同正文...",       // 对应 contractForm.content
  "status": "draft"              // 草稿状态
}
```

**预期响应：**
```json
{
  "code": 200,
  "data": {
    "id": "LX-2025-001",         // 合同编号（由后端生成）
    "name": "合同名称",
    "status": "draft",
    "createdAt": "2026-03-27T10:30:00Z"
  }
}
```

### 第2步：更新前端API接口

**文件：** `frontend/src/shared/api/legal.ts`  
**当前方法缺失：** `submitContractDraft` 需要添加存储选项

**新增接口函数：**
```typescript
// 创建/保存合同到数据库
export async function saveContractToDB(payload: {
  name: string;
  type: string;
  partyA: string;
  partyB: string;
  amount: number;
  content: string;
  status?: 'draft' | 'review' | 'signed';
}) {
  return post('/contracts', payload);
}
```

### 第3步：修改ContractDraftView中的saveDraft函数

**当前代码位置：** `frontend/src/modules/contract/views/ContractDraftView.vue` 第~310行

**旧实现（localStorage）：**
```javascript
const saveDraft = async () => {
  if (!contractForm.name || !contractForm.content) {
    alert('请输入合同名称和内容');
    return;
  }

  isSavingDraft.value = true;

  try {
    localStorage.setItem(`contract_draft_${contractForm.name}`, JSON.stringify({
      name: contractForm.name,
      type: contractForm.type,
      partyA: contractForm.partyA,
      partyB: contractForm.partyB,
      amount: contractForm.amount,
      content: contractForm.content,
      savedAt: new Date().toISOString()
    }));

    alert('草稿已保存！');
  } catch (error) {
    console.error('保存草稿失败:', error);
    alert('保存失败，请重试');
  } finally {
    isSavingDraft.value = false;
  }
};
```

**新实现（数据库）：**
```javascript
const saveDraft = async () => {
  if (!contractForm.name || !contractForm.content) {
    alert('请输入合同名称和内容');
    return;
  }

  isSavingDraft.value = true;

  try {
    const response = await saveContractToDB({
      name: contractForm.name,
      type: contractForm.type,
      partyA: contractForm.partyA,
      partyB: contractForm.partyB,
      amount: contractForm.amount,
      content: contractForm.content,
      status: 'draft'
    });

    // 显示成功提示，包含返回的合同编号
    alert(`草稿已保存！编号：${response.data?.id || '未知'}`);
    
    // 可选：返回合同列表，让用户查看
    // router.push({ name: 'ContractList' });

  } catch (error) {
    console.error('保存草稿失败:', error);
    // 错误已由http拦截器处理并转化为中文提示
    alert('保存失败，请重试');
  } finally {
    isSavingDraft.value = false;
  }
};
```

---

## 集成步骤

### 前置条件
- [x] 成员B完成数据库搭建 (`ContractEntity`, `ContractRepository`)
- [x] 成员B实现 `/api/contracts` POST 接口
- [ ] 成员B提供详细的请求/响应格式定义

### 执行流程

**第1阶段：信息收集**
1. 成员B提供接口文档
2. 核对前端发送的数据字段是否与后端字段名一致
3. 确认返回的成功/失败响应格式

**第2阶段：前端改动**
1. 在 `legal.ts` 中新增 `saveContractToDB()` 函数
2. 修改 `ContractDraftView.vue` 的 `saveDraft()` 函数
3. 检查 Axios 错误拦截器是否覆盖所有异常情况
4. 本地测试：验证网络请求、错误提示

**第3阶段：集成测试**
1. 后端启动，数据库初始化
2. 前端打开合同起草页面，填写信息
3. 点击"存草稿"，验证API调用成功
4. 检查数据库是否保存了数据
5. 打开合同台账，验证刚保存的草稿是否显示

**第4阶段：验收**
- [x] 数据成功写入数据库
- [x] 前端显示操作反馈（成功/失败提示）
- [x] 草稿在合同台账中可见
- [x] 可进一步编辑或提交审查

---

## 字段映射表

| 前端变量名 | 后端参数名 | 类型 | 备注 |
|-----------|-----------|------|------|
| contractForm.name | name | string | 合同名称 |
| contractForm.type | type | string | 合同类型 |
| contractForm.partyA | partyA | string | 甲方名称 |
| contractForm.partyB | partyB | string | 乙方名称 |
| contractForm.amount | amount | number | 合同金额 |
| contractForm.content | content | string | 合同正文 |
| (固定值) | status | string | 固定为 "draft" |

---

## 错误处理

### 预期的错误场景与处理

| 场景 | HTTP状态 | 错误信息 | 处理方式 |
|------|---------|---------|---------|
| 参数缺失 | 400 | "请求参数错误" | 拦截器自动处理 |
| 权限不足 | 403 | "暂无权限访问该资源" | 拦截器自动处理 |
| 数据库异常 | 500 | "服务器错误，请稍后重试" | 拦截器自动处理 |
| 网络超时 | - | "网络连接失败，请检查网络设置" | 拦截器自动处理 |

**Axios拦截器已完善，无需额外改动。**

---

## 协作检查清单

### 成员B需要提供
- [ ] `/api/contracts` POST 接口的详细文档
- [ ] 请求体字段名与数据类型
- [ ] 响应体字段名与数据类型
- [ ] 错误响应示例
- [ ] 合同编号生成规则（如 LX-2025-001）
- [ ] 合同表的字段定义（用于后续合同台账显示）

### 成员C需要完成
- [ ] 修改 `legal.ts`，新增 `saveContractToDB()` 函数
- [ ] 修改 `ContractDraftView.vue` 的 `saveDraft()` 函数
- [ ] 本地测试验证API调用
- [ ] 文档更新

### 联合验收
- [ ] 召开集成测试会议
- [ ] 执行端到端测试流程
- [ ] 验证数据一致性
- [ ] 签署验收单

---

## 可选增强功能

### 开发完成后可考虑的扩展

1. **自动保存**
   ```javascript
   // 每30秒自动保存一次
   setInterval(saveDraft, 30000);
   ```

2. **返回后跳转到合同台账**
   ```javascript
   router.push({ name: 'ContractList' });
   ```

3. **显示保存历史**
   - 查询该合同的所有保存版本
   - 支持版本对比、恢复

4. **草稿同步提示**
   - 若合同名相同，询问是否覆盖或创建新版本

---

## 时间估计

| 工作 | 工作量 | 时间 |
|------|--------|------|
| 信息收集与确认 | 1 | 30分钟 |
| 前端代码改动 | 2 | 1.5小时 |
| 本地测试 | 2 | 1.5小时 |
| 集成测试 | 3 | 2小时 |
| **合计** | 8 | **5.5小时** |

---

## 文档更新清单

完成集成后需要更新：
- [x] 本文档（集成方案）
- [ ] `成员C-功能完成与验证说明.md`（更新"存草稿"部分为已联调）
- [ ] README.md（如有API文档章节）

---

## 附录：完整修改代码示例

### legal.ts 新增代码

```typescript
// 保存合同到数据库
export async function saveContractToDB(payload: any) {
  return post('/contracts', payload);
}
```

### ContractDraftView.vue 修改代码

```javascript
import { saveContractToDB } from '@/shared/api/legal';

const saveDraft = async () => {
  if (!contractForm.name || !contractForm.content) {
    alert('请输入合同名称和内容');
    return;
  }

  isSavingDraft.value = true;

  try {
    const response = await saveContractToDB({
      name: contractForm.name,
      type: contractForm.type,
      partyA: contractForm.partyA,
      partyB: contractForm.partyB,
      amount: contractForm.amount,
      content: contractForm.content,
      status: 'draft'
    });

    alert(`草稿已保存！编号：${response.data?.id || ''}`);
  } catch (error: any) {
    console.error('保存草稿失败:', error);
    alert(error.message || '保存失败，请重试');
  } finally {
    isSavingDraft.value = false;
  }
};
```

---

**联系方式：** 等待成员B接口准备完毕后，按照本方案进行集成  
**预计交付：** 成员B完成数据库后的同一天内完成前端改动
