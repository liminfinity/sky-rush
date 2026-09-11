import type { Balance, PublicConfig, Round } from '../api/types';
// Tests only. The normal application contains no mock or fallback gameplay state.
export const config: PublicConfig = {
  version: 'test-v1',
  themes: {
    RED: {
      levels: 12,
      levelThresholds: [1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 4, 4.8, 5.8, 7, 8.5, 10],
    },
    GREEN: {
      levels: 9,
      levelThresholds: [1.2, 1.5, 2, 2.6, 3.4, 4.4, 5.7, 7.4, 9.5],
    },
  },
  betOptions: [
    { id: 'BASIC', stake: 10, boosterMultiplier: 1 },
    { id: 'DOUBLE', stake: 20, boosterMultiplier: 2 },
    { id: 'TRIPLE', stake: 30, boosterMultiplier: 3 },
    { id: 'QUAD', stake: 40, boosterMultiplier: 4 },
  ],
  points: {
    pointsPerLevel: 10,
    pointsCashoutBonus: 25,
    boosterActivationBonuses: { '1': 0, '2': 10, '3': 20, '4': 30 },
  },
  reward: {
    fragmentsWin: 2,
    fragmentsLoss: 1,
    fragmentsPerBonus: 5,
    bonusAmount: 5,
  },
};
export const balance: Balance = {
  wallet: { userId: 'demo', bonusBalance: 1000 },
  skyFragments: 0,
};
export const round: Round = {
  id: '00000000-0000-0000-0000-000000000099',
  theme: 'GREEN',
  betOptionId: 'TRIPLE',
  stake: 30,
  state: 'ACTIVE',
  booster: { multiplier: 3, level: 2, active: false },
  currentMultiplier: 1,
  completedLevel: 0,
  levelThresholds: config.themes.GREEN.levelThresholds,
  earnedPoints: 0,
  canCashout: false,
  payout: 0,
  cashoutMultiplier: null,
  crashMultiplier: null,
  reward: null,
  startedAt: '2026-09-11T12:00:00Z',
  cashoutAt: null,
  completedAt: null,
  serverTime: '2026-09-11T12:00:00Z',
  configVersion: 'test-v1',
  pointsRules: config.points,
};
export const flying: Round = {
  ...round,
  completedLevel: 1,
  currentMultiplier: 1.25,
  earnedPoints: 10,
  canCashout: true,
  serverTime: '2026-09-11T12:00:01Z',
};
export const cashedOut: Round = {
  ...flying,
  state: 'CASHED_OUT',
  cashoutAt: '2026-09-11T12:00:01Z',
  cashoutMultiplier: 1.25,
  payout: 37.5,
  canCashout: false,
  earnedPoints: 35,
};
export const loss: Round = {
  ...flying,
  state: 'COMPLETED_LOSS',
  canCashout: false,
  completedAt: '2026-09-11T12:00:03Z',
  serverTime: '2026-09-11T12:00:03Z',
  crashMultiplier: 1.75,
  currentMultiplier: 1.75,
  reward: { fragmentsGranted: 1, fragmentsRedeemed: 0, bonusBalanceGranted: 0 },
};
export const win: Round = {
  ...cashedOut,
  state: 'COMPLETED_WIN',
  completedAt: loss.completedAt,
  crashMultiplier: 1.75,
  serverTime: loss.serverTime,
  reward: { fragmentsGranted: 2, fragmentsRedeemed: 5, bonusBalanceGranted: 5 },
};
