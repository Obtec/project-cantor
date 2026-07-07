import client from './client.js';

// 인증 토큰을 실어 PDF를 받아온 뒤 blob URL로 새 탭에서 연다.
// 팝업 차단을 피하기 위해 클릭 시점에 빈 창을 먼저 열어 둔다.
export async function openPdfInNewTab(path) {
  const win = window.open('', '_blank');
  try {
    const res = await client.get(path, { responseType: 'blob' });
    const blob = new Blob([res.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    if (win) win.location = url;
    else window.open(url, '_blank');
    // 새 탭이 로드를 시작한 뒤 해제한다.
    setTimeout(() => window.URL.revokeObjectURL(url), 60000);
  } catch (err) {
    if (win) win.close();
    throw err;
  }
}
