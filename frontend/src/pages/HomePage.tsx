import { useSocial } from '../features/social/useSocial';
import { SocialFeed, AchievementReveal } from '../features/social/SocialPanels';
import '../features/social/social.css';
import { CosmeticsContext } from '../features/profile/Cosmetics';
import { ProfilePanel, UnlockReveal } from '../features/profile/ProfilePanel';
import { useProfile } from '../features/profile/useProfile';
import { isCompleted } from '../api/types';
import '../features/profile/profile.css';
import { useState } from 'react';
import { GameIcon } from '../components/GameIcon';
import { IntegrityProof } from '../features/game/IntegrityProof';
import { AppShell } from '../components/AppShell';
import { Modal } from '../components/Modal';
import { Notice } from '../components/Notice';
import { useGame } from '../features/game/useGame';
import { BetSelection } from '../features/betting/BetSelection';
import { FlightScene } from '../features/game/FlightScene';
import { FlightPanel } from '../features/game/FlightPanel';
import { ResultScreen } from '../features/game/ResultScreen';
import { HistoryList } from '../features/history/HistoryList';
import { Rules } from '../features/rules/Rules';

import { ThemeSelection } from '../features/betting/ThemeSelection';
import { LiveRanking } from '../features/competition/LiveRanking';
import { TournamentTable } from '../features/competition/TournamentTable';
import { AdminPanel } from '../features/admin/AdminPanel';
import { UpsellPrompt } from '../features/upsell/UpsellPrompt';
import { TicketInventory } from '../features/upsell/TicketInventory';
import { useGameAudio } from '../features/game/useGameAudio';
export function HomePage({
  onLogout,
  evaluator = false,
}: {
  onLogout?: () => void;
  evaluator?: boolean;
}) {
  const game = useGame();
  const profile = useProfile(
    game.round && isCompleted(game.round) ? game.round.id : undefined,
  );
  const [dialog, setDialog] = useState<
    'profile' | 'history' | 'rules' | 'tournament' | 'admin' | 'proof' | null
  >(null);
  const social = useSocial(
    game.round && isCompleted(game.round) ? game.round.id : undefined,
    dialog === 'profile',
  );
  const [entered, setEntered] = useState(false),
    [upsellOpen, setUpsellOpen] = useState(false),
    [ticketRevision, setTicketRevision] = useState(0);
  const audio = useGameAudio(game.round);
  return (
    <CosmeticsContext.Provider
      value={
        profile.profile?.collection ?? {
          balloonSkin: 'classic',
          profileFrame: 'plain',
        }
      }
    >
      <AppShell
        theme={game.theme}
        balance={game.balance}
        user={game.user}
        mode={
          game.round
            ? game.resultVisible
              ? 'result'
              : 'flight'
            : entered
              ? 'bets'
              : 'entry'
        }
        onProfile={() => {
          setDialog('profile');
          void profile.refresh();
        }}
        onHistory={() => setDialog('history')}
        onRules={() => setDialog('rules')}
        tools={
          <>
            <button
              type="button"
              className="hud-button"
              aria-label="Рейтинг"
              title="Рейтинг"
              onClick={() => setDialog('tournament')}
            >
              <GameIcon name="trophy" />
            </button>
            <button
              type="button"
              className="hud-button"
              aria-label={audio.enabled ? 'Звук: вкл' : 'Звук: выкл'}
              title="Звук"
              aria-pressed={audio.enabled}
              onClick={() => void audio.toggle()}
            >
              <GameIcon name={audio.enabled ? 'sound' : 'mute'} />
            </button>
            {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions -- Delegates native button clicks (including keyboard activation); summary remains the native toggle. */}
            <details
              className="utility-menu"
              onClick={(event) => {
                if (
                  event.target instanceof Element &&
                  event.target.closest('button')
                )
                  event.currentTarget.removeAttribute('open');
              }}
            >
              <summary aria-label="Ещё" title="Ещё">
                <GameIcon name="more" />
              </summary>
              <div>
                {entered && !game.round && !game.pending && (
                  <button type="button" onClick={() => setEntered(false)}>
                    Выбрать шар
                  </button>
                )}
                {game.round && (
                  <button type="button" onClick={() => setDialog('proof')}>
                    Проверка полёта
                  </button>
                )}
                {evaluator && (
                  <>
                    <button type="button" onClick={() => setDialog('admin')}>
                      Настройки игры
                    </button>
                  </>
                )}
                {onLogout && (
                  <button type="button" onClick={onLogout}>
                    Выйти
                  </button>
                )}
              </div>
            </details>
          </>
        }
      >
        {audio.error && <Notice subtle>{audio.error}</Notice>}
        {game.dataError && (
          <Notice retry={() => void game.refreshData()}>
            {game.dataError}
          </Notice>
        )}
        {game.error && <Notice>{game.error}</Notice>}
        {game.round ? (
          <>
            {game.polling.commandError && (
              <Notice>{game.polling.commandError}</Notice>
            )}
            {game.polling.connectionError && (
              <Notice subtle>Соединяемся…</Notice>
            )}
            {game.polling.missing ? (
              <div className="loading-screen">
                <h1>Полёт не найден</h1>
                <button
                  type="button"
                  className="primary-button"
                  onClick={game.playAgain}
                >
                  Выбрать ставку
                </button>
              </div>
            ) : game.resultVisible ? (
              <ResultScreen
                unlock={
                  <>
                    <UnlockReveal
                      collection={profile.profile?.collection}
                      roundId={game.round.id}
                    />
                    <AchievementReveal
                      items={social.achievements}
                      daily={social.daily}
                      roundId={game.round.id}
                    />
                  </>
                }
                round={game.round}
                balance={game.balance}
                playAgain={() => {
                  setEntered(true);
                  game.playAgain();
                }}
                paused={dialog !== null || upsellOpen || game.starting}
                rewardRules={game.config?.reward}
                repeat={() => void game.start(game.round!)}
                repeating={game.starting}
                onIdle={() => {
                  setEntered(false);
                  game.playAgain();
                }}
              />
            ) : (
              <>
                <LiveRanking ranking={game.round.ranking} />
                <div className="flight-layout">
                  <FlightScene round={game.round} />
                  <FlightPanel
                    round={game.round}
                    cashout={game.polling.cashout}
                    busy={game.polling.busy}
                    reconnecting={!!game.polling.connectionError}
                  />
                </div>
              </>
            )}
          </>
        ) : game.recovering ? (
          <div className="loading-screen">
            <h1>Возвращаем полёт…</h1>
            <button
              type="button"
              className="quiet-button"
              onClick={() => void game.restore()}
            >
              Повторить
            </button>
          </div>
        ) : game.loading || !game.config ? (
          <div className="loading-screen" role="status">
            <span className="loading-orbit" />
            <h1>{game.loading ? 'Загружаем…' : 'Не удалось подключиться'}</h1>
            {!game.loading && (
              <button
                type="button"
                className="primary-button"
                onClick={() => void game.refreshData()}
              >
                Повторить
              </button>
            )}
          </div>
        ) : !entered && !game.pending ? (
          <ThemeSelection
            config={game.config}
            choose={(theme) => {
              game.chooseTheme(theme);
              setEntered(true);
            }}
          />
        ) : (
          <BetSelection
            config={game.config}
            balance={game.balance}
            theme={game.theme}
            selected={game.selected}
            chooseTheme={game.chooseTheme}
            select={game.setSelected}
            start={() => void game.start()}
            starting={game.starting}
            pending={!!game.pending}
            unavailable={!!game.dataError}
          />
        )}
        {game.resultVisible && game.round && (
          <UpsellPrompt
            round={game.round}
            onOpen={setUpsellOpen}
            onBalance={() => {
              void game.refreshData();
              setTicketRevision((v) => v + 1);
            }}
          />
        )}
        {!game.round && <TicketInventory revision={ticketRevision} />}
        <SocialFeed data={social.activity} error={social.error} />
        {dialog && (
          <Modal
            title={
              {
                profile: 'Профиль',
                history: 'История',
                rules: 'Правила',
                tournament: 'Рейтинг',
                admin: 'Настройки игры',
                proof: 'Проверка полёта',
              }[dialog]
            }
            onClose={() => setDialog(null)}
          >
            {dialog === 'profile' ? (
              <ProfilePanel
                achievements={social.achievements}
                daily={social.daily}
                profile={profile.profile}
                error={profile.error}
                loading={profile.loading}
                equip={(id) => void profile.equip(id)}
                busy={profile.equipping}
                retry={() => void profile.refresh()}
              />
            ) : dialog === 'history' ? (
              <HistoryList revision={game.historyRevision} />
            ) : dialog === 'rules' ? (
              <Rules config={game.config} />
            ) : dialog === 'tournament' ? (
              <TournamentTable />
            ) : dialog === 'admin' ? (
              <AdminPanel onSaved={() => void game.refreshData()} />
            ) : (
              game.round && (
                <>
                  <p>
                    Каждый уровень: {game.round.pointsRules.pointsPerLevel}{' '}
                    очков.
                  </p>
                  <IntegrityProof round={game.round} />
                </>
              )
            )}
          </Modal>
        )}
      </AppShell>
    </CosmeticsContext.Provider>
  );
}
