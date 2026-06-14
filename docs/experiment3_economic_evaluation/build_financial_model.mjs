import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const outDir = path.resolve(".");
const workbook = Workbook.create();

const theme = {
  navy: "#17324D",
  blue: "#2563EB",
  teal: "#0F766E",
  green: "#15803D",
  red: "#B91C1C",
  amber: "#FDE68A",
  gray: "#F3F4F6",
  line: "#D1D5DB",
  white: "#FFFFFF",
  black: "#111827",
};

const years = [2026, 2027, 2028, 2029, 2030];
const fmtCny = '"¥"#,##0;[Red]("¥"#,##0);-';
const fmtPct = "0.0%;[Red](0.0%);-";
const fmtNum = "#,##0;[Red](#,##0);-";
const fmtDec = "0.00";

function styleTitle(sheet, range, title) {
  const r = sheet.getRange(range);
  r.merge();
  r.values = [[title]];
  r.format = {
    fill: theme.navy,
    font: { bold: true, color: theme.white, size: 15 },
    wrapText: true,
  };
}

function styleHeader(range, fill = theme.teal) {
  range.format = {
    fill,
    font: { bold: true, color: theme.white },
    borders: { preset: "all", style: "thin", color: theme.line },
    wrapText: true,
  };
}

function styleTable(range) {
  range.format = {
    borders: { preset: "all", style: "thin", color: theme.line },
    wrapText: true,
  };
}

function setWidths(sheet, widths) {
  widths.forEach((width, idx) => {
    sheet.getRangeByIndexes(0, idx, 1, 1).format.columnWidthPx = width;
  });
}

const summary = workbook.worksheets.add("Summary");
const inputs = workbook.worksheets.add("Inputs");
const revenue = workbook.worksheets.add("Revenue");
const costs = workbook.worksheets.add("Costs");
const cash = workbook.worksheets.add("CashFlow");
const sens = workbook.worksheets.add("Sensitivity");
const checks = workbook.worksheets.add("Checks");
const sources = workbook.worksheets.add("Sources");

for (const s of [summary, inputs, revenue, costs, cash, sens, checks, sources]) {
  s.showGridLines = false;
}

// Inputs
styleTitle(inputs, "A1:F1", "LexAI Economic Evaluation - Inputs and Assumptions");
inputs.getRange("A3:F3").values = [["Item", "Value", "Unit", "Source ID", "Type", "Notes"]];
styleHeader(inputs.getRange("A3:F3"));
inputs.getRange("A4:F17").values = [
  ["Valuation date", "2026-06-14", "date", "S0", "Model", "Based on experiment preparation date."],
  ["Currency", "CNY", "", "S0", "Model", "All amounts are RMB, excluding VAT unless noted."],
  ["Forecast period", "2026-2030", "years", "S0", "Model", "Five-year post-launch forecast."],
  ["Discount rate", 0.1, "%", "A1", "Input", "Base case cost of capital for a small software project."],
  ["Corporate income tax", 0.25, "%", "S6", "Statutory", "Conservative statutory rate."],
  ["VAT rate for software/SaaS service", 0.06, "%", "S5", "Statutory", "Used for tax table reference, model revenue is net of VAT."],
  ["Initial investment", 250000, "CNY", "A2", "Input", "Prototype economic value + commercialization hardening + initial working capital."],
  ["Prototype WBS effort", 508, "hours", "P1", "Project", "From LexAI ProjectCharter.md."],
  ["Prototype direct budget", 3500, "CNY", "P1", "Project", "From LexAI ProjectCharter.md."],
  ["Opportunity cost per dev hour", 80, "CNY/hour", "A3", "Input", "Student/developer opportunity-cost assumption."],
  ["AI cost per task", 0.08, "CNY/task", "S4/A4", "Input", "Derived from Hunyuan token price and 8k-token average task plus legal-data call buffer."],
  ["Payment channel fee", 0.01, "% of revenue", "A5", "Input", "Conservative platform/payment processing assumption."],
  ["Working capital ratio", 0.05, "% of revenue", "A6", "Input", "Receivables, deposits and prepaid cloud resources."],
  ["Useful life of initial intangible asset", 3, "years", "A7", "Input", "Straight-line amortization for software platform asset."],
];
styleTable(inputs.getRange("A4:F17"));
inputs.getRange("B7:B9").format.numberFormat = fmtPct;
inputs.getRange("B10:B13").format.numberFormat = fmtCny;
inputs.getRange("B14:B17").format.numberFormat = fmtNum;
inputs.freezePanes.freezeRows(3);
setWidths(inputs, [250, 140, 110, 90, 110, 420]);

inputs.getRange("A20:G20").values = [["Year", "Individual paid accounts", "Individual price / month", "SME accounts", "SME price / month", "Enterprise contracts", "Enterprise price / year"]];
styleHeader(inputs.getRange("A20:G20"));
inputs.getRange("A21:G25").values = [
  [2026, 500, 29, 50, 199, 2, 12000],
  [2027, 1500, 29, 160, 199, 5, 12000],
  [2028, 3500, 29, 360, 199, 10, 12000],
  [2029, 6000, 29, 650, 199, 18, 12000],
  [2030, 9000, 29, 950, 199, 28, 12000],
];
styleTable(inputs.getRange("A21:G25"));
inputs.getRange("C21:C25").format.numberFormat = fmtCny;
inputs.getRange("E21:E25").format.numberFormat = fmtCny;
inputs.getRange("G21:G25").format.numberFormat = fmtCny;

inputs.getRange("A28:G28").values = [["Year", "Cloud & infra", "Payroll / operation", "Compliance & legal review", "Marketing % revenue", "Capex", "Depreciation / amortization"]];
styleHeader(inputs.getRange("A28:G28"));
inputs.getRange("A29:G33").values = [
  [2026, 36000, 260000, 30000, 0.15, 50000, 83333],
  [2027, 72000, 520000, 60000, 0.15, 30000, 83333],
  [2028, 120000, 900000, 100000, 0.15, 40000, 83334],
  [2029, 180000, 1350000, 150000, 0.12, 60000, 0],
  [2030, 260000, 1850000, 220000, 0.12, 80000, 0],
];
styleTable(inputs.getRange("A29:G33"));
inputs.getRange("B29:D33").format.numberFormat = fmtCny;
inputs.getRange("E29:E33").format.numberFormat = fmtPct;
inputs.getRange("F29:G33").format.numberFormat = fmtCny;

// Revenue
styleTitle(revenue, "A1:L1", "Revenue Model");
revenue.getRange("A3:L3").values = [["Year", "Individual accounts", "Price / month", "Individual revenue", "SME accounts", "SME price / month", "SME revenue", "Enterprise contracts", "Enterprise price / year", "Enterprise revenue", "Total revenue", "AI tasks"]];
styleHeader(revenue.getRange("A3:L3"));
revenue.getRange("A4:A8").values = years.map((y) => [y]);
revenue.getRange("B4:C8").formulas = years.map((_, i) => [`=Inputs!B${21 + i}`, `=Inputs!C${21 + i}`]);
revenue.getRange("D4:D8").formulas = years.map((_, i) => [`=B${4 + i}*C${4 + i}*12`]);
revenue.getRange("E4:F8").formulas = years.map((_, i) => [`=Inputs!D${21 + i}`, `=Inputs!E${21 + i}`]);
revenue.getRange("G4:G8").formulas = years.map((_, i) => [`=E${4 + i}*F${4 + i}*12`]);
revenue.getRange("H4:I8").formulas = years.map((_, i) => [`=Inputs!F${21 + i}`, `=Inputs!G${21 + i}`]);
revenue.getRange("J4:J8").formulas = years.map((_, i) => [`=H${4 + i}*I${4 + i}`]);
revenue.getRange("K4:K8").formulas = years.map((_, i) => [`=D${4 + i}+G${4 + i}+J${4 + i}`]);
revenue.getRange("L4:L8").formulas = years.map((_, i) => [`=(B${4 + i}*8+E${4 + i}*50+H${4 + i}*600)*12`]);
styleTable(revenue.getRange("A4:L8"));
revenue.getRange("C4:D8").format.numberFormat = fmtCny;
revenue.getRange("F4:G8").format.numberFormat = fmtCny;
revenue.getRange("I4:K8").format.numberFormat = fmtCny;
revenue.getRange("L4:L8").format.numberFormat = fmtNum;
revenue.freezePanes.freezeRows(3);
setWidths(revenue, [80, 130, 120, 140, 100, 120, 140, 140, 140, 150, 140, 120]);

const revChart = revenue.charts.add("line", revenue.getRange("A3:K8"));
revChart.title = "Revenue by Stream";
revChart.hasLegend = true;
revChart.xAxis = { axisType: "textAxis" };
revChart.yAxis = { numberFormatCode: "¥#,##0" };
revChart.setPosition("B11", "K28");

// Costs
styleTitle(costs, "A1:J1", "Operating Cost Model");
costs.getRange("A3:J3").values = [["Year", "AI/legal data variable cost", "Payment fee", "Cloud & infra", "Payroll / operation", "Marketing", "Compliance", "Total cash operating cost", "Depreciation / amortization", "EBITDA"]];
styleHeader(costs.getRange("A3:J3"));
costs.getRange("A4:A8").values = years.map((y) => [y]);
costs.getRange("B4:B8").formulas = years.map((_, i) => [`=Revenue!L${4 + i}*Inputs!B14`]);
costs.getRange("C4:C8").formulas = years.map((_, i) => [`=Revenue!K${4 + i}*Inputs!B15`]);
costs.getRange("D4:D8").formulas = years.map((_, i) => [`=Inputs!B${29 + i}`]);
costs.getRange("E4:E8").formulas = years.map((_, i) => [`=Inputs!C${29 + i}`]);
costs.getRange("F4:F8").formulas = years.map((_, i) => [`=Revenue!K${4 + i}*Inputs!E${29 + i}`]);
costs.getRange("G4:G8").formulas = years.map((_, i) => [`=Inputs!D${29 + i}`]);
costs.getRange("H4:H8").formulas = years.map((_, i) => [`=SUM(B${4 + i}:G${4 + i})`]);
costs.getRange("I4:I8").formulas = years.map((_, i) => [`=Inputs!G${29 + i}`]);
costs.getRange("J4:J8").formulas = years.map((_, i) => [`=Revenue!K${4 + i}-H${4 + i}`]);
styleTable(costs.getRange("A4:J8"));
costs.getRange("B4:J8").format.numberFormat = fmtCny;
costs.freezePanes.freezeRows(3);
setWidths(costs, [80, 165, 120, 130, 150, 120, 130, 185, 185, 120]);

// Cash flow
styleTitle(cash, "A1:G1", "Pre-financing Project Cash Flow");
cash.getRange("A3:G3").values = [["Item", "2025 investment", ...years.map(String)]];
styleHeader(cash.getRange("A3:G3"));
cash.getRange("A4:A14").values = [
  ["Revenue"],
  ["Cash operating cost"],
  ["EBITDA"],
  ["Depreciation / amortization"],
  ["EBIT"],
  ["Income tax"],
  ["Capex"],
  ["Net working capital"],
  ["Change in NWC"],
  ["Free cash flow"],
  ["Cumulative FCF"],
];
cash.getRange("B4:B12").values = Array.from({ length: 9 }, () => [0]);
cash.getRange("B13").formulas = [["=-Inputs!B10"]];
cash.getRange("B14").formulas = [["=B13"]];
cash.getRange("C4:G4").formulas = years.map((_, i) => `=Revenue!K${4 + i}`).reduce((a, v) => [[...a[0], v]], [[]]);
cash.getRange("C5:G5").formulas = years.map((_, i) => `=Costs!H${4 + i}`).reduce((a, v) => [[...a[0], v]], [[]]);
cash.getRange("C6:G6").formulas = [["=C4-C5", "=D4-D5", "=E4-E5", "=F4-F5", "=G4-G5"]];
cash.getRange("C7:G7").formulas = years.map((_, i) => `=Costs!I${4 + i}`).reduce((a, v) => [[...a[0], v]], [[]]);
cash.getRange("C8:G8").formulas = [["=C6-C7", "=D6-D7", "=E6-E7", "=F6-F7", "=G6-G7"]];
cash.getRange("C9:G9").formulas = [["=MAX(0,C8*Inputs!B8)", "=MAX(0,D8*Inputs!B8)", "=MAX(0,E8*Inputs!B8)", "=MAX(0,F8*Inputs!B8)", "=MAX(0,G8*Inputs!B8)"]];
cash.getRange("C10:G10").formulas = years.map((_, i) => `=Inputs!F${29 + i}`).reduce((a, v) => [[...a[0], v]], [[]]);
cash.getRange("C11:G11").formulas = [["=C4*Inputs!B16", "=D4*Inputs!B16", "=E4*Inputs!B16", "=F4*Inputs!B16", "=G4*Inputs!B16"]];
cash.getRange("C12:G12").formulas = [["=C11-B11", "=D11-C11", "=E11-D11", "=F11-E11", "=G11-F11"]];
cash.getRange("C13:G13").formulas = [["=C8-C9+C7-C10-C12", "=D8-D9+D7-D10-D12", "=E8-E9+E7-E10-E12", "=F8-F9+F7-F10-F12", "=G8-G9+G7-G10-G12"]];
cash.getRange("C14:G14").formulas = [["=B14+C13", "=C14+D13", "=D14+E13", "=E14+F13", "=F14+G13"]];
styleTable(cash.getRange("A4:G14"));
cash.getRange("B4:G14").format.numberFormat = fmtCny;
cash.getRange("A13:G14").format = {
  fill: theme.gray,
  font: { bold: true, color: theme.black },
  borders: { preset: "all", style: "thin", color: theme.line },
};
cash.freezePanes.freezeRows(3);
setWidths(cash, [210, 135, 120, 120, 120, 120, 120]);

const cfChart = cash.charts.add("bar", cash.getRange("A13:G13"));
cfChart.title = "Free Cash Flow";
cfChart.hasLegend = false;
cfChart.yAxis = { numberFormatCode: "¥#,##0" };
cfChart.setPosition("A17", "G32");

// Summary
styleTitle(summary, "A1:H1", "LexAI Economic Evaluation Summary");
summary.getRange("A3:B12").values = [
  ["Model status", ""],
  ["Project", "LexAI Intelligent Legal Assistance System"],
  ["Evaluation perspective", "Commercialization of course-project prototype"],
  ["Initial investment", ""],
  ["Total 5-year revenue", ""],
  ["Discount rate", ""],
  ["NPV", ""],
  ["IRR", ""],
  ["Dynamic payback period", ""],
  ["Decision", ""],
];
summary.getRange("B3").formulas = [["=Checks!F11"]];
summary.getRange("B6").formulas = [["=-CashFlow!B13"]];
summary.getRange("B7").formulas = [["=SUM(CashFlow!C4:G4)"]];
summary.getRange("B8").formulas = [["=Inputs!B7"]];
summary.getRange("B9").formulas = [["=NPV(B8,CashFlow!C13:G13)+CashFlow!B13"]];
summary.getRange("B10").formulas = [["=IRR(CashFlow!B13:G13)"]];
summary.getRange("B11").formulas = [["=2+ABS(CashFlow!D14)/CashFlow!E13"]];
summary.getRange("B12").formulas = [["=IF(AND(B9>0,B10>B8),\"Feasible: proceed with controlled commercialization\",\"Not feasible under base case\")"]];
styleTable(summary.getRange("A3:B12"));
summary.getRange("A3:A12").format = { fill: theme.gray, font: { bold: true }, borders: { preset: "all", style: "thin", color: theme.line } };
summary.getRange("B6:B7").format.numberFormat = fmtCny;
summary.getRange("B8:B10").format.numberFormat = fmtPct;
summary.getRange("B9").format.numberFormat = fmtCny;
summary.getRange("B11").format.numberFormat = fmtDec;
summary.getRange("B3").format = { fill: theme.green, font: { bold: true, color: theme.white } };
summary.getRange("A15:H15").values = [["Year", "Revenue", "Cash Opex", "EBITDA", "Free Cash Flow", "Cumulative FCF", "Paying Users", "AI Tasks"]];
styleHeader(summary.getRange("A15:H15"));
summary.getRange("A16:A20").values = years.map((y) => [y]);
summary.getRange("B16:B20").formulas = years.map((_, i) => [`=Revenue!K${4 + i}`]);
summary.getRange("C16:C20").formulas = years.map((_, i) => [`=Costs!H${4 + i}`]);
summary.getRange("D16:D20").formulas = years.map((_, i) => [`=Costs!J${4 + i}`]);
summary.getRange("E16:E20").formulas = years.map((_, i) => [`=CashFlow!${String.fromCharCode(67 + i)}13`]);
summary.getRange("F16:F20").formulas = years.map((_, i) => [`=CashFlow!${String.fromCharCode(67 + i)}14`]);
summary.getRange("G16:G20").formulas = years.map((_, i) => [`=Revenue!B${4 + i}+Revenue!E${4 + i}+Revenue!H${4 + i}`]);
summary.getRange("H16:H20").formulas = years.map((_, i) => [`=Revenue!L${4 + i}`]);
styleTable(summary.getRange("A16:H20"));
summary.getRange("B16:F20").format.numberFormat = fmtCny;
summary.getRange("G16:H20").format.numberFormat = fmtNum;
setWidths(summary, [180, 135, 135, 135, 145, 150, 120, 115]);
summary.freezePanes.freezeRows(15);

const summaryChart = summary.charts.add("line", summary.getRange("A15:F20"));
summaryChart.title = "Revenue, EBITDA and FCF Trend";
summaryChart.hasLegend = true;
summaryChart.xAxis = { axisType: "textAxis" };
summaryChart.yAxis = { numberFormatCode: "¥#,##0" };
summaryChart.setPosition("D3", "H13");

// Sensitivity
styleTitle(sens, "A1:G1", "Sensitivity Analysis");
sens.getRange("A3:C3").values = [["Revenue factor", "Cost factor", "NPV @ 10%"]];
styleHeader(sens.getRange("A3:C3"));
const factors = [
  [0.85, 0.85], [0.85, 1.0], [0.85, 1.15],
  [1.0, 0.85], [1.0, 1.0], [1.0, 1.15],
  [1.15, 0.85], [1.15, 1.0], [1.15, 1.15],
];
sens.getRange("A4:B12").values = factors;
for (let r = 4; r <= 12; r++) {
  const revF = `A${r}`;
  const costF = `B${r}`;
  const flows = years.map((_, i) => {
    const row = 4 + i;
    const col = String.fromCharCode(67 + i);
    const rev = `(Revenue!K${row}*${revF})`;
    const opex = `((Costs!B${row}+Costs!D${row}+Costs!E${row}+Costs!G${row})*${costF}+Costs!C${row}*${revF}+${rev}*Inputs!E${29 + i})`;
    const ebitda = `(${rev}-${opex})`;
    const ebit = `(${ebitda}-Costs!I${row})`;
    const taxCalc = `MAX(0,${ebit}*Inputs!B8)`;
    const nwc = `${rev}*Inputs!B16`;
    const prevNwc = i === 0 ? "0" : `(Revenue!K${row - 1}*${revF}*Inputs!B16)`;
    const delta = `(${nwc}-${prevNwc})`;
    const capex = `(Inputs!F${29 + i}*${costF})`;
    return `(${ebit}-${taxCalc}+Costs!I${row}-${capex}-${delta})`;
  });
  sens.getRange(`C${r}`).formulas = [[`=NPV(Inputs!B7,${flows.join(",")})-Inputs!B10*${costF}`]];
}
styleTable(sens.getRange("A4:C12"));
sens.getRange("A4:B12").format.numberFormat = fmtPct;
sens.getRange("C4:C12").format.numberFormat = fmtCny;
sens.getRange("E3:H3").values = [["NPV matrix", "Cost -15%", "Base cost", "Cost +15%"]];
styleHeader(sens.getRange("E3:H3"));
sens.getRange("E4:E6").values = [["Revenue -15%"], ["Base revenue"], ["Revenue +15%"]];
sens.getRange("F4:H6").formulas = [
  ["=C4", "=C5", "=C6"],
  ["=C7", "=C8", "=C9"],
  ["=C10", "=C11", "=C12"],
];
styleTable(sens.getRange("E4:H6"));
sens.getRange("F4:H6").format.numberFormat = fmtCny;
setWidths(sens, [120, 110, 140, 40, 150, 130, 130, 130]);

// Checks
styleTitle(checks, "A1:F1", "Model Checks");
checks.getRange("A3:F3").values = [["Check", "Actual", "Expected", "Difference", "Tolerance", "Status"]];
styleHeader(checks.getRange("A3:F3"));
checks.getRange("A4:A10").values = [
  ["Revenue total ties to streams"],
  ["Cash opex ties to components"],
  ["Initial investment entered"],
  ["FCF row equals components"],
  ["NPV is positive"],
  ["IRR exceeds discount rate"],
  ["Sensitivity downside NPV positive"],
];
checks.getRange("B4:F10").formulas = [
  ["=SUM(Revenue!K4:K8)", "=SUM(Revenue!D4:D8)+SUM(Revenue!G4:G8)+SUM(Revenue!J4:J8)", "=B4-C4", "1", "=IF(ABS(D4)<=E4,\"OK\",\"Review\")"],
  ["=SUM(Costs!H4:H8)", "=SUM(Costs!B4:G8)", "=B5-C5", "1", "=IF(ABS(D5)<=E5,\"OK\",\"Review\")"],
  ["=-CashFlow!B13", "=Inputs!B10", "=B6-C6", "1", "=IF(ABS(D6)<=E6,\"OK\",\"Review\")"],
  ["=SUM(CashFlow!C13:G13)", "=SUM(CashFlow!C8:G8)-SUM(CashFlow!C9:G9)+SUM(CashFlow!C7:G7)-SUM(CashFlow!C10:G10)-SUM(CashFlow!C12:G12)", "=B7-C7", "1", "=IF(ABS(D7)<=E7,\"OK\",\"Review\")"],
  ["=Summary!B9", "0", "=B8-C8", "0", "=IF(B8>C8,\"OK\",\"Review\")"],
  ["=Summary!B10", "=Summary!B8", "=B9-C9", "0", "=IF(B9>C9,\"OK\",\"Review\")"],
  ["=Sensitivity!H4", "0", "=B10-C10", "0", "=IF(B10>C10,\"OK\",\"Review\")"],
];
checks.getRange("A11:E11").values = [["Overall model status", "", "", "", ""]];
checks.getRange("F11").formulas = [["=IF(COUNTIF(F4:F10,\"Review\")=0,\"OK\",\"Review\")"]];
styleTable(checks.getRange("A4:F11"));
checks.getRange("B4:E8").format.numberFormat = fmtCny;
checks.getRange("B9:D9").format.numberFormat = fmtPct;
checks.getRange("F4:F11").format = { fill: theme.green, font: { bold: true, color: theme.white }, borders: { preset: "all", style: "thin", color: theme.line } };
setWidths(checks, [240, 140, 140, 130, 110, 100]);

// Sources
styleTitle(sources, "A1:F1", "Sources and Audit Trail");
sources.getRange("A3:F3").values = [["Source ID", "Item", "Value used", "As of / period", "Source", "Notes"]];
styleHeader(sources.getRange("A3:F3"));
sources.getRange("A4:F15").values = [
  ["P1", "LexAI WBS and prototype budget", "508 hours; ¥3,500 direct budget", "2026-06", "D:\\Desktop\\LexAI\\docs\\ProjectCharter.md", "Existing project charter."],
  ["P2", "LexAI implemented modules and tech stack", "Vue 3 + Spring Boot 3 + Java 21; AI/RAG/legal workflows", "2026-06", "D:\\Desktop\\LexAI\\README.md; docs\\项目简介.md", "Used for scope and cost drivers."],
  ["S1", "China court case volume", "2025 courts accepted 37.486 million trial/enforcement cases", "2025", "https://www.moj.gov.cn/pub/sfbgw/zwxxgkztzl/2026nianzhuanti/2026qglh0206/lhjj20260206/lhjjyw20260206/202603/t20260310_532524.html", "Indicates sustained legal-service demand."],
  ["S2", "China legal-service supply", "83万 lawyers; 4.5万 law firms", "2025-09", "https://www.moj.gov.cn/pub/sfbgw/sfbxwfbhzb/2025nsfbfbh/202509gzlwcsswgh/202509gzlwcsswgh_wzzb/", "Legal-service market scale."],
  ["S3", "Global legal AI market", "USD 1.45B in 2024; USD 3.90B by 2030; 17.3% CAGR", "2024-2030", "https://www.grandviewresearch.com/industry-analysis/legal-ai-market-report", "External market growth benchmark."],
  ["S4", "Tencent Hunyuan token price", "HY 2.0 Instruct input ¥3.18/m tokens, output ¥7.95/m tokens for <=32k", "2026-06", "https://cloud.tencent.com/document/product/1729/97731", "Used to derive AI task cost."],
  ["S5", "VAT for services/intangibles", "6%", "effective 2026", "https://fgk.chinatax.gov.cn/zcfgk/c100009/c5237365/content.html", "Revenue model is net of VAT; report includes VAT reference."],
  ["S6", "Corporate income tax", "25%", "current law", "https://fgk.chinatax.gov.cn/zcfgk/c100009/c5193018/content.html", "Conservative income-tax assumption."],
  ["A1", "Discount rate", "10%", "Experiment assumption", "Analyst assumption", "Small software-project opportunity cost and risk premium."],
  ["A2", "Commercialization investment", "¥250,000", "Experiment assumption", "Analyst assumption", "Prototype value, hardening, compliance, launch and working capital."],
  ["A3", "Developer opportunity cost", "¥80/hour", "Experiment assumption", "Analyst assumption", "Used to value student project effort economically."],
  ["A4", "Average AI task profile", "8k tokens + legal data buffer", "Experiment assumption", "Analyst assumption", "Legal consultation/review mixed workload."],
];
styleTable(sources.getRange("A4:F15"));
setWidths(sources, [85, 220, 210, 120, 520, 310]);
sources.freezePanes.freezeRows(3);

for (const ws of [summary, inputs, revenue, costs, cash, sens, checks, sources]) {
  const used = ws.getUsedRange();
  if (used) {
    used.format.font = { name: "Microsoft YaHei", size: 10, color: theme.black };
  }
}

// Re-apply key title/header styles after global font pass.
for (const [ws, range, title] of [
  [summary, "A1:H1", "LexAI Economic Evaluation Summary"],
  [inputs, "A1:F1", "LexAI Economic Evaluation - Inputs and Assumptions"],
  [revenue, "A1:L1", "Revenue Model"],
  [costs, "A1:J1", "Operating Cost Model"],
  [cash, "A1:G1", "Pre-financing Project Cash Flow"],
  [sens, "A1:G1", "Sensitivity Analysis"],
  [checks, "A1:F1", "Model Checks"],
  [sources, "A1:F1", "Sources and Audit Trail"],
]) {
  styleTitle(ws, range, title);
}

await workbook.inspect({ kind: "table", range: "Summary!A1:H20", include: "values,formulas", tableMaxRows: 22, tableMaxCols: 8 });
const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 50 },
  summary: "formula error scan",
});
console.log(errors.ndjson);

for (const sheetName of ["Summary", "Inputs", "Revenue", "Costs", "CashFlow", "Sensitivity", "Checks", "Sources"]) {
  const preview = await workbook.render({ sheetName, autoCrop: "all", scale: 1, format: "png" });
  await fs.writeFile(path.join(outDir, `${sheetName}.png`), new Uint8Array(await preview.arrayBuffer()));
}

const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(path.join(outDir, "LexAI_经济评价财务模型.xlsx"));
console.log("Saved LexAI_经济评价财务模型.xlsx");
