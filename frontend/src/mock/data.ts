// 목업(밴드 아카이브.dc.html)의 시드 데이터를 도메인 타입으로 옮긴 것.
// 백엔드 연동 시 이 파일을 api 호출로 대체한다.

import type { Band, MediaItem, Member, ScheduleEvent, Song, User } from '../types';

const INK = '#201e1d';
const N500 = '#9b9797';
const N600 = '#7d7979';
const N700 = '#605d5d';
const N800 = '#444141';

export const CURRENT_USER: User = { id: 'u-hs', name: '김현수', initial: '수' };

export const BANDS: Band[] = [
  { id: 'b1', name: '김현수와 발톱때들', initial: '발', memberCount: 5, myRole: 'owner', note: '합주곡 3 · 위시 4' },
  { id: 'b2', name: '라이트하우스', initial: '라', memberCount: 6, myRole: 'member', note: '합주곡 7 · 위시 2' },
  { id: 'b3', name: '목요일의 소음', initial: '목', memberCount: 4, myRole: 'member', note: '합주곡 5 · 위시 9' },
];

export const MEMBERS: Member[] = [
  { id: 'm1', bandId: 'b1', name: '김현수', initial: '수', part: '보컬 / 기타', role: 'owner', avatarColor: INK },
  { id: 'm2', bandId: 'b1', name: '전환', initial: '환', part: '기타', role: 'member', avatarColor: N800 },
  { id: 'm3', bandId: 'b1', name: '송민호', initial: '호', part: '베이스', role: 'member', avatarColor: N700 },
  { id: 'm4', bandId: 'b1', name: '김시훈', initial: '훈', part: '드럼', role: 'member', avatarColor: N600 },
  { id: 'm5', bandId: 'b1', name: '윤수빈', initial: '빈', part: '키보드', role: 'member', avatarColor: N500 },
];

export const SONGS: Song[] = [
  {
    id: 's1', bandId: 'b1', title: 'ring ring ring', artist: '설', status: 'WISHLIST', sourceType: 'SEARCH',
    proposer: '윤수빈', memo: '인트로 신스는 건반으로 대체. 2절 후 브릿지 4마디 늘려서 기타 솔로.',
    referenceVideoUrl: 'https://youtu.be/ringringring',
    sessions: { 보컬: 1, 기타: 2, 베이스: 1, 드럼: 1, 건반: 1 },
    votes: 5, votedByMe: true, addedOrder: 1, assignments: {},
  },
  {
    id: 's2', bandId: 'b1', title: '밤편지', artist: '아이유', status: 'WISHLIST', sourceType: 'SEARCH',
    proposer: '전환', memo: '키 A♭ → G로 내려서 시도. 드럼은 브러시.',
    referenceVideoUrl: 'https://youtu.be/bam',
    sessions: { 보컬: 1, 기타: 1, 베이스: 1, 드럼: 1, 건반: 1 },
    votes: 4, votedByMe: false, addedOrder: 2, assignments: {},
  },
  {
    id: 's3', bandId: 'b1', title: '여수 밤바다', artist: '버스커버스커', status: 'WISHLIST', sourceType: 'MANUAL',
    proposer: '송민호', memo: '앙코르용. 관객 떼창 구간 남겨두기.', referenceVideoUrl: '',
    sessions: { 보컬: 1, 기타: 2, 베이스: 1, 드럼: 1 },
    votes: 3, votedByMe: false, addedOrder: 3, assignments: {},
  },
  {
    id: 's4', bandId: 'b1', title: 'Creep', artist: 'Radiohead', status: 'WISHLIST', sourceType: 'SEARCH',
    proposer: '김시훈', memo: '기타 크런치 톤 세팅 필요. 후반 샤우팅 파트 논의.',
    referenceVideoUrl: 'https://youtu.be/creep',
    sessions: { 보컬: 1, 기타: 2, 베이스: 1, 드럼: 1 },
    votes: 2, votedByMe: false, addedOrder: 4, assignments: {},
  },
  {
    id: 'p1', bandId: 'b1', title: '청춘', artist: '김창완밴드', status: 'CONFIRMED', sourceType: 'MANUAL',
    proposer: '김현수', memo: '엔딩곡 고정. 아웃트로 반복 8마디.', referenceVideoUrl: '',
    sessions: { 보컬: 1, 기타: 2, 베이스: 1, 드럼: 1 },
    votes: 6, votedByMe: true, addedOrder: 5,
    assignments: { '보컬#1': '김현수', '기타#1': '전환', '기타#2': '', '베이스#1': '송민호', '드럼#1': '김시훈' },
  },
  {
    id: 'p2', bandId: 'b1', title: '사랑은 은하수 다방에서', artist: '10cm', status: 'CONFIRMED', sourceType: 'SEARCH',
    proposer: '윤수빈', memo: '건반 인트로부터 시작. 8/29 합주에서 러닝.',
    referenceVideoUrl: 'https://youtu.be/10cm',
    sessions: { 보컬: 1, 기타: 1, 베이스: 1, 드럼: 1, 건반: 1 },
    votes: 5, votedByMe: false, addedOrder: 6,
    assignments: { '보컬#1': '윤수빈', '기타#1': '전환', '베이스#1': '송민호', '드럼#1': '김시훈', '건반#1': '윤수빈' },
  },
  {
    id: 'p3', bandId: 'b1', title: '한 잔의 추억', artist: '이장희', status: 'CONFIRMED', sourceType: 'MANUAL',
    proposer: '송민호', memo: '오프닝. 템포 살짝 업.', referenceVideoUrl: '',
    sessions: { 보컬: 1, 기타: 1, 베이스: 1, 드럼: 1 },
    votes: 4, votedByMe: false, addedOrder: 7,
    assignments: { '보컬#1': '김현수', '기타#1': '전환', '베이스#1': '', '드럼#1': '김시훈' },
  },
];

export const SCHEDULES: ScheduleEvent[] = [
  { id: 'e1', bandId: 'b1', type: 'REHEARSAL', day: 8, dow: '토', time: '19:00–22:00', title: '파트 연습 · ring ring ring', place: '합정 뮤즈 스튜디오 2관' },
  { id: 'e2', bandId: 'b1', type: 'REHEARSAL', day: 15, dow: '토', time: '19:00–22:00', title: '전곡 러닝', place: '홍대 사운드 스튜디오 B' },
  { id: 'e3', bandId: 'b1', type: 'REHEARSAL', day: 22, dow: '토', time: '18:00–21:00', title: '녹음용 합주', place: '홍대 사운드 스튜디오 B' },
  { id: 'e4', bandId: 'b1', type: 'REHEARSAL', day: 29, dow: '토', time: '19:00–22:00', title: '정기 합주 · 셋리스트 점검', place: '홍대 사운드 스튜디오 B' },
  { id: 'e5', bandId: 'b1', type: 'PERFORMANCE', day: 30, dow: '일', time: '17:30 입장', title: '여름 합동공연 3곡', place: 'club FF' },
];

export const MEDIA: MediaItem[] = [
  { id: 'v1', bandId: 'b1', title: '8/22 녹음용 합주 풀영상', source: 'YouTube', date: '8월 22일', kind: '합주', visibility: '멤버만', scheduleDay: 22 },
  { id: 'v2', bandId: 'b1', title: 'ring ring ring 파트 연습', source: 'Google Drive', date: '8월 8일', kind: '합주', visibility: '멤버만', scheduleDay: 8 },
  { id: 'v3', bandId: 'b1', title: '여름 합동공연 3곡', source: 'YouTube', date: '7월 26일', kind: '공연', visibility: '링크 공개', scheduleDay: null },
  { id: 'v4', bandId: 'b1', title: 'club FF 앙코르', source: 'YouTube', date: '6월 14일', kind: '공연', visibility: '링크 공개', scheduleDay: null },
];

/** 곡 검색 모달용 외부 카탈로그 목(Spotify 등 대체) */
export const SONG_CATALOG: { title: string; artist: string }[] = [
  { title: 'ring ring ring', artist: '설' },
  { title: '밤편지', artist: '아이유' },
  { title: '봄날', artist: '방탄소년단' },
  { title: '리무진', artist: "BE'O" },
  { title: 'Bohemian Rhapsody', artist: 'Queen' },
  { title: '사랑은 은하수 다방에서', artist: '10cm' },
  { title: '헤븐', artist: '에일리' },
  { title: 'Antifreeze', artist: '검정치마' },
  { title: '너의 밤은 어떠니', artist: '스탠딩 에그' },
  { title: 'Creep', artist: 'Radiohead' },
];
