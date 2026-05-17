import QRCode from "qrcode";

export interface ReceiptData {
  receipt_number: string;
  receipt_token?: string;
  sponsor_name: string;
  sponsor_email?: string;
  recipient_name: string;
  recipient_role: string;
  recipient_location: string;
  amount: number;
  currency: string;
  status: string;
  razorpay_order_id?: string;
  razorpay_payment_id: string;
  issued_at: string;
  message: string;
}

export function formatMoney(amount: number, currency = "INR"): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function formatTimestamp(isoDate: string): string {
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(isoDate));
}

export function buildVerificationUrl(receipt: ReceiptData, origin = window.location.origin): string {
  const url = new URL("/receipt", origin);
  if (receipt.receipt_token) {
    url.searchParams.set("t", receipt.receipt_token);
  }
  return url.toString();
}

export function createQrMatrix(value: string): { size: number; data: ArrayLike<boolean | number> } {
  const qr = QRCode.create(value, { errorCorrectionLevel: "M" });
  return { size: qr.modules.size, data: qr.modules.data };
}

export function renderQr(elementId: string, value: string): void {
  const el = document.getElementById(elementId);
  if (!el) return;

  const matrix = createQrMatrix(value);
  el.innerHTML = "";
  el.style.gridTemplateColumns = `repeat(${matrix.size}, minmax(0, 1fr))`;
  el.style.gridTemplateRows = `repeat(${matrix.size}, minmax(0, 1fr))`;

  for (const active of Array.from(matrix.data)) {
    const cell = document.createElement("span");
    cell.className = active ? "qr-cell qr-cell-active" : "qr-cell";
    el.appendChild(cell);
  }
}

function toPdfText(value: string): string {
  return value
    .replace(/[^\x20-\x7E]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function escapePdf(value: string): string {
  return toPdfText(value)
    .replace(/\\/g, "\\\\")
    .replace(/\(/g, "\\(")
    .replace(/\)/g, "\\)");
}

function formatPdfMoney(amount: number, currency: string): string {
  return `${currency} ${amount.toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

export function buildReceiptPdf(receipt: ReceiptData): Blob {
  const streamParts: string[] = [];
  const color = {
    bg: "1 1 1",
    panel: "0.985 0.985 0.99",
    panelGreen: "0.94 0.985 0.955",
    panelBlue: "0.94 0.965 1",
    panelOrange: "1 0.965 0.91",
    panelRose: "1 0.95 0.955",
    mutedPanel: "0.955 0.958 0.965",
    border: "0.82 0.83 0.85",
    text: "0.03 0.035 0.04",
    muted: "0.36 0.38 0.42",
    green: "0 0.54 0.28",
    orange: "0.93 0.42 0",
    red: "0.78 0.1 0.12",
    qr: "0 0 0",
  };

  const fillRect = (x: number, y: number, width: number, height: number, fill: string): void => {
    streamParts.push(`${fill} rg ${x} ${y} ${width} ${height} re f`);
  };

  const strokeRect = (x: number, y: number, width: number, height: number, stroke = color.border): void => {
    streamParts.push(`${stroke} RG ${x} ${y} ${width} ${height} re S`);
  };

  const drawText = (text: string, x: number, y: number, font: "F1" | "F2", size: number, fill = color.text): void => {
    streamParts.push(`${fill} rg BT /${font} ${size} Tf 1 0 0 1 ${x} ${y} Tm (${escapePdf(text)}) Tj ET`);
  };

  const drawCenteredText = (text: string, centerX: number, y: number, font: "F1" | "F2", size: number, fill = color.text): void => {
    const estimatedWidth = toPdfText(text).length * size * (font === "F2" ? 0.62 : 0.52);
    drawText(text, centerX - estimatedWidth / 2, y, font, size, fill);
  };

  const wrapLines = (text: string, maxChars: number, maxLines = 3): string[] => {
    const words = toPdfText(text).split(" ").filter(Boolean);
    const lines: string[] = [];
    let line = "";

    for (const word of words) {
      const next = line ? `${line} ${word}` : word;
      if (next.length > maxChars && line) {
        lines.push(line);
        line = word;
      } else {
        line = next;
      }
      if (lines.length >= maxLines) break;
    }
    if (line && lines.length < maxLines) lines.push(line);
    return lines;
  };

  const drawWrappedText = (text: string, x: number, y: number, maxChars: number, lineHeight: number, font: "F1" | "F2", size: number, fill = color.text, maxLines = 4): void => {
    wrapLines(text, maxChars, maxLines).forEach((row, index) => {
      drawText(row, x, y - index * lineHeight, font, size, fill);
    });
  };

  const drawLabelValue = (label: string, value: string, x: number, y: number, width: number, height = 54, fill = color.panel): void => {
    fillRect(x, y - height, width, height, fill);
    strokeRect(x, y - height, width, height);
    drawText(label, x + 12, y - 16, "F2", 7.8, color.muted);
    drawWrappedText(value || "-", x + 12, y - 32, width > 230 ? 31 : 22, 10.5, "F1", 9.3, color.text, 2);
  };

  const drawQr = (value: string, x: number, y: number, size: number): void => {
    const matrix = createQrMatrix(value);
    fillRect(x, y, size, size, color.bg);
    strokeRect(x, y, size, size);
    const quietZone = 4;
    const cell = size / (matrix.size + quietZone * 2);
    const dot = cell * 0.84;
    const inset = (cell - dot) / 2;

    Array.from(matrix.data).forEach((active, index) => {
      if (!active) return;
      const col = index % matrix.size;
      const row = Math.floor(index / matrix.size);
      fillRect(
        x + (col + quietZone) * cell + inset,
        y + size - (row + quietZone + 1) * cell + inset,
        dot,
        dot,
        color.qr,
      );
    });
  };

  const orderID = receipt.razorpay_order_id || "ORDER_ID_UNAVAILABLE";
  const verificationUrl = buildVerificationUrl(receipt);

  fillRect(0, 0, 612, 792, color.bg);
  strokeRect(32, 32, 548, 728);
  fillRect(32, 718, 548, 42, color.mutedPanel);
  fillRect(32, 714, 548, 4, color.green);
  fillRect(48, 737, 8, 8, color.red);
  fillRect(63, 737, 8, 8, color.orange);
  fillRect(78, 737, 8, 8, color.green);

  drawText("ZR_SYSTEMS", 48, 684, "F2", 26);
  drawText("SPONSORSHIP PAYMENT RECEIPT", 48, 665, "F2", 9.5, color.muted);
  drawText("CONTRIBUTION CONFIRMED", 390, 684, "F2", 10.5, color.green);
  drawText(`STATUS: ${receipt.status.toUpperCase()}`, 390, 666, "F2", 9.5, color.green);

  drawLabelValue("RECEIPT NO", receipt.receipt_number, 48, 626, 250, 50, color.panelBlue);
  drawLabelValue("ISSUED AT", formatTimestamp(receipt.issued_at), 314, 626, 250, 50, color.panelOrange);

  drawLabelValue("SPONSOR", receipt.sponsor_name, 48, 552, 250, 56, color.panelGreen);
  drawLabelValue("SPONSOR EMAIL", receipt.sponsor_email || "Not provided", 314, 552, 250, 56, color.panelRose);
  drawLabelValue("RECEIVED BY", receipt.recipient_name, 48, 476, 250, 56, color.panelBlue);
  drawLabelValue("RECIPIENT ROLE", `${receipt.recipient_role} / ${receipt.recipient_location}`, 314, 476, 250, 56, color.panelOrange);

  fillRect(48, 288, 320, 96, color.panelGreen);
  strokeRect(48, 288, 320, 96);
  drawText("CONTRIBUTION AMOUNT", 66, 356, "F2", 8.5, color.muted);
  drawText(formatPdfMoney(receipt.amount, receipt.currency), 66, 326, "F2", 23, color.green);
  drawText("METHOD", 246, 356, "F2", 8.5, color.muted);
  drawText("RAZORPAY", 246, 336, "F2", 11.5, color.text);
  drawText("MODE", 246, 314, "F2", 8.5, color.muted);
  drawText("ONE-TIME SUPPORT", 246, 300, "F1", 8.5, color.text);

  fillRect(388, 288, 176, 96, color.panelBlue);
  strokeRect(388, 288, 176, 96);
  drawQr(verificationUrl, 438, 300, 76);
  drawCenteredText("PRIVATE VERIFY QR", 476, 293, "F2", 7.5, color.muted);

  drawLabelValue("RAZORPAY ORDER ID", orderID, 48, 258, 250, 52, color.panelOrange);
  drawLabelValue("RAZORPAY PAYMENT ID", receipt.razorpay_payment_id, 314, 258, 250, 52, color.panelRose);

  fillRect(48, 124, 516, 70, color.panelGreen);
  strokeRect(48, 124, 516, 70);
  drawText("THANK YOU", 64, 172, "F2", 8.8, color.green);
  drawWrappedText(receipt.message, 64, 154, 74, 11, "F1", 8.7, color.text, 3);

  fillRect(48, 78, 516, 34, "1 0.966 0.91");
  strokeRect(48, 78, 516, 34);
  drawText("NOTE", 64, 97, "F2", 7.5, color.muted);
  drawText("This receipt confirms a voluntary one-time sponsorship payment processed through Razorpay.", 100, 97, "F1", 8, color.text);
  drawText("Keep the receipt number and payment ID available for support.", 100, 86, "F1", 7.5, color.muted);

  const stream = streamParts.join("\n");
  const objects = [
    "<< /Type /Catalog /Pages 2 0 R >>",
    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>",
    `<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`,
    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
    "<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold >>",
  ];

  let pdf = "%PDF-1.4\n";
  const xref: number[] = [0];

  objects.forEach((object, index) => {
    xref.push(pdf.length);
    pdf += `${index + 1} 0 obj\n${object}\nendobj\n`;
  });

  const xrefStart = pdf.length;
  pdf += `xref\n0 ${objects.length + 1}\n`;
  pdf += "0000000000 65535 f \n";
  for (let i = 1; i < xref.length; i += 1) {
    pdf += `${String(xref[i]).padStart(10, "0")} 00000 n \n`;
  }
  pdf += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefStart}\n%%EOF`;

  return new Blob([pdf], { type: "application/pdf" });
}

export function downloadReceiptPdf(receipt: ReceiptData): void {
  const url = URL.createObjectURL(buildReceiptPdf(receipt));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `${receipt.receipt_number}.pdf`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
