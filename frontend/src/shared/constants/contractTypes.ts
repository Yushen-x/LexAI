export interface ContractTypeOption {
  value: string;
  label: string;
}

export const CONTRACT_TYPE_OPTIONS: ContractTypeOption[] = [
  { value: '采购合同', label: '采购合同' },
  { value: '服务合同', label: '服务合同' },
  { value: '技术开发', label: '技术开发合同' },
  { value: '工程合同', label: '工程合同' },
  { value: '运维服务', label: '运维服务合同' },
  { value: '保密/期权', label: '保密协议 / 期权条款' },
  { value: '租赁合同', label: '租赁合同' },
  { value: '劳动合同', label: '劳动合同' }
];

export const CONTRACT_TYPE_VALUES = CONTRACT_TYPE_OPTIONS.map((item) => item.value);
