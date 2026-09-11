// Mirrors the public Java DTOs. Monetary values are displayed, never used to compute outcomes.
export type Theme = 'RED' | 'GREEN';
export type RoundState =
  'ACTIVE' | 'CASHED_OUT' | 'COMPLETED_WIN' | 'COMPLETED_LOSS';
export interface User {
  id: string;
  displayName: string;
  createdAt: string;
}
export interface Balance {
  wallet: { userId: string; bonusBalance: number };
  skyFragments: number;
}
export interface BetOption {
  id: string;
  stake: number;
  boosterMultiplier: number;
}
export interface PointsRules {
  pointsPerLevel: number;
  pointsCashoutBonus: number;
  boosterActivationBonuses: Record<string, number>;
}
export interface RewardRules {
  fragmentsWin: number;
  fragmentsLoss: number;
  fragmentsPerBonus: number;
  bonusAmount: number;
}
export interface PublicConfig {
  version: string;
  themes: Record<Theme, { levelThresholds: number[]; levels: number }>;
  betOptions: BetOption[];
  points: PointsRules;
  reward: RewardRules;
}
export interface Reward {
  fragmentsGranted: number;
  fragmentsRedeemed: number;
  bonusBalanceGranted: number;
}
export interface Round {
  id: string;
  theme: Theme;
  betOptionId: string;
  stake: number;
  state: RoundState;
  booster: { multiplier: number; level: number | null; active: boolean };
  currentMultiplier: number;
  completedLevel: number;
  levelThresholds: number[];
  earnedPoints: number;
  canCashout: boolean;
  payout: number;
  cashoutMultiplier: number | null;
  crashMultiplier: number | null;
  reward: Reward | null;
  startedAt: string;
  cashoutAt: string | null;
  completedAt: string | null;
  integrity?: {
    algorithm: string;
    commitment: string;
    reveal: string | null;
  } | null;
  ranking?: Ranking;
  serverTime: string;
  configVersion: string;
  pointsRules: PointsRules;
}
export interface StartRequest {
  requestId: string;
  theme: Theme;
  betOptionId: string;
}
export interface History {
  rounds: Round[];
  limit: number;
  offset: number;
}
export const isCompleted = (round: Round) =>
  round.state === 'COMPLETED_WIN' || round.state === 'COMPLETED_LOSS';

export interface RankingEntry {
  id: string;
  name: string;
  points: number;
  position: number;
  currentPlayer: boolean;
}
export interface Ranking {
  entries: RankingEntry[];
}
export interface Tournament {
  name: string;
  status: string;
  endsAt: string;
  remainingSeconds: number;
  entries: RankingEntry[];
  currentPlayer: RankingEntry;
}
export interface GameConfiguration {
  themes: Record<
    Theme,
    { levelThresholds: number[]; boosterPositionWeights: number[] }
  >;
  betOptions: BetOption[];
  crashModel: { minMultiplier: number; maxMultiplier: number };
  growth: { multiplierPerSecond: number };
  points: PointsRules;
  reward: RewardRules;
}
export interface PrototypeConfiguration {
  gameId: string;
  name: string;
  type: string;
  active: boolean;
  upsell: {
    enabled: boolean;
    minWinAmount: number;
    popupTimeoutSeconds: number;
    ticketPrice: number;
    maxTickets: number;
    payoutFraction: number;
  };
}
export interface ConfigSnapshot<T> {
  version: string;
  configuration: T;
}
export interface TicketOffer {
  id: string;
  roundId: string;
  quantity: number;
  unitPrice: number;
  total: number;
  expiresAt: string;
  status: 'OFFERED' | 'PURCHASED' | 'DECLINED' | 'EXPIRED';
  remainingSeconds: number;
}
export interface Tickets {
  offer: TicketOffer | null;
  tickets: number;
}

export interface Account extends User {
  username: string;
  evaluator: boolean;
}

export interface CosmeticItem {
  id: string;
  kind: 'BALLOON' | 'FRAME';
  name: string;
  fragmentsRequired: number;
  unlocked: boolean;
  equipped: boolean;
  unlockedAt: string | null;
  unlockedRoundId: string | null;
}
export interface Collection {
  lifetimeFragments: number;
  nextThreshold: number;
  fragmentsToNext: number;
  nextItemName: string | null;
  balloonSkin: string;
  profileFrame: string;
  items: CosmeticItem[];
}
export interface PersonalRecords {
  totalRounds: number;
  successfulCashouts: number;
  losses: number;
  winRate: number;
  highestCashoutMultiplier: number | null;
  highestCrashMultiplier: number | null;
  biggestPayout: number;
  highestLevel: number;
  strongestBooster: number;
  totalPoints: number;
  totalFragments: number;
  currentWinStreak: number;
  bestWinStreak: number;
}
export interface Profile {
  account: Account;
  bonusBalance: number;
  fragmentBalance: number;
  tournament: RankingEntry;
  records: PersonalRecords;
  collection: Collection;
}
export interface Achievement {
  id: string;
  name: string;
  description: string;
  unlockedAt: string | null;
  roundId: string | null;
}
export interface DailyChallenge {
  date: string;
  kind: string;
  description: string;
  progress: number;
  target: number;
  completed: boolean;
  rewardFragments: number;
  rewardRoundId: string | null;
}
export interface ActivityEvent {
  id: string;
  displayName: string;
  kind: 'CASHOUT' | 'BOOSTER' | 'ACHIEVEMENT';
  value: string;
  createdAt: string;
}
export interface SocialActivity {
  online: number;
  presenceWindowSeconds: number;
  events: ActivityEvent[];
}
