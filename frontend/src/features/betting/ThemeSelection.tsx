import type { PublicConfig, Theme } from '../../api/types';
import { Balloon } from '../../components/Balloon';
import { SkyWorld } from '../../components/SkyWorld';
export function ThemeSelection({
  config,
  choose,
}: {
  config: PublicConfig;
  choose: (theme: Theme) => void;
}) {
  return (
    <section className="theme-entry">
      <SkyWorld theme="GREEN" className="entry-day" />
      <SkyWorld theme="RED" className="entry-sunset" />
      <h1>Выбери шар</h1>
      <div className="theme-choices">
        {(['GREEN', 'RED'] as const).map((theme) => (
          <button
            type="button"
            key={theme}
            className={`theme-choice theme-${theme.toLowerCase()}`}
            onClick={() => choose(theme)}
            aria-label={`Выбрать ${theme}`}
          >
            <div className="choice-balloon">
              <Balloon />
            </div>
            <strong>{theme === 'GREEN' ? 'Зелёный' : 'Красный'}</strong>
            <span>{config.themes[theme].levels} уровней</span>
          </button>
        ))}
      </div>
    </section>
  );
}
