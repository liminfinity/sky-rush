const amountFormat = new Intl.NumberFormat('ru-RU', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const multiplierFormat = new Intl.NumberFormat('ru-RU', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
});
export const amount = (value: number) => amountFormat.format(value);
export const multiplier = (value: number | null) =>
  value === null ? '—' : `×${multiplierFormat.format(value)}`;
export const integer = (value: number) =>
  new Intl.NumberFormat('ru-RU').format(value);
export const dateTime = (value: string) =>
  new Intl.DateTimeFormat('ru-RU', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
export const themeName = (theme: string) =>
  theme === 'RED' ? 'Закатный полёт' : 'Бирюзовый бриз';
