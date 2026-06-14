from pathlib import Path
import re
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    SimpleDocTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
    PageBreak,
)


ROOT = Path(__file__).resolve().parent
MD_PATH = ROOT / "LexAI_实验三_经济评价报告.md"
PDF_PATH = ROOT / "LexAI_实验三_经济评价报告.pdf"


def inline_md(text: str) -> str:
    text = escape(text)
    text = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)
    return text


def is_table_line(line: str) -> bool:
    return line.strip().startswith("|") and line.strip().endswith("|")


def parse_table(lines):
    rows = []
    for line in lines:
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if all(re.fullmatch(r":?-{3,}:?", c or "") for c in cells):
            continue
        rows.append(cells)
    return rows


def col_widths_for(rows, usable_width):
    if not rows:
        return []
    n = max(len(r) for r in rows)
    max_lens = [1] * n
    for row in rows:
        for i, cell in enumerate(row):
            max_lens[i] = max(max_lens[i], min(len(cell), 28))
    total = sum(max_lens)
    return [usable_width * l / total for l in max_lens]


def build_pdf():
    font_name = "MSYH"
    pdfmetrics.registerFont(TTFont(font_name, r"C:\Windows\Fonts\msyh.ttc"))
    styles = getSampleStyleSheet()
    styles.add(
        ParagraphStyle(
            name="CnTitle",
            fontName=font_name,
            fontSize=20,
            leading=26,
            alignment=TA_CENTER,
            spaceAfter=10,
        )
    )
    styles.add(
        ParagraphStyle(
            name="CnH1",
            fontName=font_name,
            fontSize=15,
            leading=20,
            textColor=colors.HexColor("#17324D"),
            spaceBefore=10,
            spaceAfter=6,
        )
    )
    styles.add(
        ParagraphStyle(
            name="CnH2",
            fontName=font_name,
            fontSize=11.2,
            leading=15,
            textColor=colors.HexColor("#111827"),
            spaceBefore=6,
            spaceAfter=4,
        )
    )
    styles.add(
        ParagraphStyle(
            name="CnBody",
            fontName=font_name,
            fontSize=10.5,
            leading=16,
            alignment=TA_LEFT,
            firstLineIndent=18,
            spaceAfter=5,
        )
    )
    styles.add(
        ParagraphStyle(
            name="CnSmall",
            fontName=font_name,
            fontSize=8.4,
            leading=11,
            alignment=TA_LEFT,
            spaceAfter=2,
        )
    )
    styles.add(
        ParagraphStyle(
            name="CnList",
            fontName=font_name,
            fontSize=10.5,
            leading=15,
            leftIndent=14,
            firstLineIndent=-10,
            spaceAfter=3,
        )
    )

    doc = SimpleDocTemplate(
        str(PDF_PATH),
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=16 * mm,
        bottomMargin=16 * mm,
        title="LexAI 实验三 经济评价报告",
    )
    usable_width = A4[0] - doc.leftMargin - doc.rightMargin
    story = []

    lines = MD_PATH.read_text(encoding="utf-8").splitlines()
    i = 0
    while i < len(lines):
        raw = lines[i]
        line = raw.strip()
        if not line:
            story.append(Spacer(1, 3))
            i += 1
            continue
        if line.startswith("# "):
            story.append(Paragraph(inline_md(line[2:]), styles["CnTitle"]))
            story.append(Spacer(1, 8))
            i += 1
            continue
        if line.startswith("## "):
            story.append(Paragraph(inline_md(line[3:]), styles["CnH1"]))
            i += 1
            continue
        if line.startswith("### "):
            story.append(Paragraph(inline_md(line[4:]), styles["CnH2"]))
            i += 1
            continue
        if is_table_line(line):
            table_lines = []
            while i < len(lines) and is_table_line(lines[i]):
                table_lines.append(lines[i])
                i += 1
            rows = parse_table(table_lines)
            data = []
            for r, row in enumerate(rows):
                style = styles["CnSmall"]
                data.append([Paragraph(inline_md(cell), style) for cell in row])
            t = Table(data, colWidths=col_widths_for(rows, usable_width), repeatRows=1)
            t.setStyle(
                TableStyle(
                    [
                        ("FONTNAME", (0, 0), (-1, -1), font_name),
                        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0F766E")),
                        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#CBD5E1")),
                        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                        ("LEFTPADDING", (0, 0), (-1, -1), 4),
                        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                        ("TOPPADDING", (0, 0), (-1, -1), 4),
                        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                    ]
                )
            )
            story.append(t)
            story.append(Spacer(1, 7))
            continue
        if re.match(r"^\d+\.\s+", line):
            story.append(Paragraph(inline_md(line), styles["CnList"]))
            i += 1
            continue
        story.append(Paragraph(inline_md(line), styles["CnBody"]))
        i += 1

    def footer(canvas, doc_obj):
        canvas.saveState()
        canvas.setFont(font_name, 8)
        canvas.setFillColor(colors.HexColor("#475569"))
        canvas.drawCentredString(A4[0] / 2, 9 * mm, f"LexAI 经济评价报告 · 第 {doc_obj.page} 页")
        canvas.restoreState()

    doc.build(story, onFirstPage=footer, onLaterPages=footer)
    print(PDF_PATH)


if __name__ == "__main__":
    build_pdf()
