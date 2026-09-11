import { createContext, useContext } from 'react';
export const CosmeticsContext = createContext({
  balloonSkin: 'classic',
  profileFrame: 'plain',
});
export const useCosmetics = () => useContext(CosmeticsContext);
export function PlayerAvatar({
  name,
  frame,
}: {
  name: string;
  frame?: string;
}) {
  const equipped = useCosmetics();
  return (
    <span
      aria-hidden="true"
      className={`player-avatar frame-${frame ?? equipped.profileFrame}`}
      data-frame={frame ?? equipped.profileFrame}
    >
      {name.slice(0, 1)}
    </span>
  );
}
