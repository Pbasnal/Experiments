import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ChapterJumpSheet from './ChapterJumpSheet';

const chapters = [
  { slug: 'c1', title: 'One', chapterNumber: 1, listedAt: null },
  { slug: 'c2', title: 'Two', chapterNumber: 2, listedAt: null },
];

function Harness({ onCloseSpy }: { onCloseSpy: () => void }) {
  const [open, setOpen] = useState(false);
  return (
    <div>
      <button type="button" onClick={() => setOpen(true)}>
        Open
      </button>
      <ChapterJumpSheet
        open={open}
        chapters={chapters}
        activeSlug="c1"
        onClose={() => {
          setOpen(false);
          onCloseSpy();
        }}
        onSelect={() => setOpen(false)}
      />
    </div>
  );
}

describe('ChapterJumpSheet focus trap', () => {
  it('returns focus to the opener when closed', async () => {
    const user = userEvent.setup();
    const onCloseSpy = vi.fn();
    render(<Harness onCloseSpy={onCloseSpy} />);

    const openBtn = screen.getByRole('button', { name: 'Open' });
    openBtn.focus();
    await user.click(openBtn);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /close chapter list/i })).toHaveFocus();

    await user.keyboard('{Escape}');
    expect(onCloseSpy).toHaveBeenCalled();
    expect(openBtn).toHaveFocus();
  });
});
