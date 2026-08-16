import { useLocation } from 'react-router-dom';

export const FEEDBACK_FORM_URL =
  'https://docs.google.com/forms/d/e/1FAIpQLSd2ZC6GS-92iA7TkiijEL_DkCnGaT3OYyr8sqMTkHa3Vm6b3Q/viewform';

export default function FeedbackFab() {
  const { pathname } = useLocation();
  const onChapter = /^\/read\/s\/[^/]+\/c\//.test(pathname);

  return (
    <a
      className={`feedback-fab${onChapter ? ' feedback-fab--subtle' : ''}`}
      href={FEEDBACK_FORM_URL}
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Send feedback"
      title="Send feedback"
    >
      Feedback
    </a>
  );
}
