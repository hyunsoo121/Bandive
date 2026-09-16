/** 이미 올라간 이미지(로고·배너 등) URL 을 CropModal 에 다시 넣을 수 있는 File 로 변환한다 —
 * 새 파일을 고르지 않고 지금 걸려있는 사진의 위치/확대만 다시 조정할 때 쓴다. */
export async function urlToFile(url: string, filename: string): Promise<File> {
  const res = await fetch(url);
  if (!res.ok) throw new Error('이미지를 불러오지 못했습니다.');
  const blob = await res.blob();
  return new File([blob], filename, { type: blob.type || 'image/jpeg' });
}
