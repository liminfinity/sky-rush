export function Notice({
  children,
  retry,
  subtle = false,
}: {
  children: React.ReactNode;
  retry?: () => void;
  subtle?: boolean;
}) {
  return (
    <div
      className={`notice ${subtle ? 'subtle' : ''}`}
      role={subtle ? 'status' : 'alert'}
    >
      <span>{children}</span>
      {retry && (
        <button type="button" onClick={retry}>
          Повторить
        </button>
      )}
    </div>
  );
}
