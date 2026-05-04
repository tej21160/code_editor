import CodeMirror from '@uiw/react-codemirror';
import { javascript } from '@codemirror/lang-javascript';
import { python } from '@codemirror/lang-python';
import { java } from '@codemirror/lang-java';
import { oneDark } from '@codemirror/theme-one-dark';

const languageExtensions = {
  javascript: javascript(),
  python: python(),
  java: java(),
};

export default function CodeEditor({ code, onChange, language = 'javascript' }) {
  return (
    <CodeMirror
      value={code}
      height="60vh"
      theme={oneDark}
      extensions={[languageExtensions[language] || javascript()]}
      onChange={(value) => onChange(value)}
      style={{
        fontSize: '14px',
        borderRadius: '8px',
        overflow: 'hidden',
        border: '1px solid #333'
      }}
    />
  );
}
