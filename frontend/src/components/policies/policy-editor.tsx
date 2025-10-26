/**
 * Policy Editor Component
 *
 * @description Monaco-based code editor for Rego policies with syntax highlighting
 */

'use client'

import { useEffect, useRef } from 'react'
import Editor, { OnMount } from '@monaco-editor/react'
import type { editor } from 'monaco-editor'

interface PolicyEditorProps {
  value: string
  onChange: (value: string) => void
  readOnly?: boolean
  height?: string
}

/**
 * Policy editor component with Rego syntax highlighting
 */
export function PolicyEditor({
  value,
  onChange,
  readOnly = false,
  height = '500px',
}: PolicyEditorProps) {
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null)

  /**
   * Handle editor mount
   */
  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor

    // Register Rego language
    monaco.languages.register({ id: 'rego' })

    // Define Rego syntax highlighting
    monaco.languages.setMonarchTokensProvider('rego', {
      keywords: [
        'package',
        'import',
        'default',
        'as',
        'with',
        'some',
        'else',
        'not',
        'in',
        'every',
      ],
      operators: [
        '=',
        '==',
        '!=',
        '<',
        '>',
        '<=',
        '>=',
        ':=',
        '|',
        '&',
        '+',
        '-',
        '*',
        '/',
        '%',
      ],
      symbols: /[=><!~?:&|+\-*\/\^%]+/,
      tokenizer: {
        root: [
          // Comments
          [/#.*$/, 'comment'],

          // Keywords
          [
            /[a-zA-Z_]\w*/,
            {
              cases: {
                '@keywords': 'keyword',
                '@default': 'identifier',
              },
            },
          ],

          // Numbers
          [/\d+/, 'number'],

          // Strings
          [/"([^"\\]|\\.)*$/, 'string.invalid'], // non-teminated string
          [/"/, { token: 'string.quote', bracket: '@open', next: '@string' }],

          // Delimiters and operators
          [/[{}()\[\]]/, '@brackets'],
          [/@symbols/, { cases: { '@operators': 'operator', '@default': '' } }],

          // Whitespace
          { include: '@whitespace' },
        ],

        string: [
          [/[^\\"]+/, 'string'],
          [/\\./, 'string.escape'],
          [/"/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
        ],

        whitespace: [
          [/[ \t\r\n]+/, 'white'],
          [/#.*$/, 'comment'],
        ],
      },
    })

    // Define Rego language configuration
    monaco.languages.setLanguageConfiguration('rego', {
      comments: {
        lineComment: '#',
      },
      brackets: [
        ['{', '}'],
        ['[', ']'],
        ['(', ')'],
      ],
      autoClosingPairs: [
        { open: '{', close: '}' },
        { open: '[', close: ']' },
        { open: '(', close: ')' },
        { open: '"', close: '"' },
      ],
      surroundingPairs: [
        { open: '{', close: '}' },
        { open: '[', close: ']' },
        { open: '(', close: ')' },
        { open: '"', close: '"' },
      ],
    })

    // Set editor theme for Rego
    monaco.editor.defineTheme('rego-theme', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'comment', foreground: '6A9955' },
        { token: 'keyword', foreground: 'C586C0' },
        { token: 'identifier', foreground: '9CDCFE' },
        { token: 'string', foreground: 'CE9178' },
        { token: 'number', foreground: 'B5CEA8' },
        { token: 'operator', foreground: 'D4D4D4' },
      ],
      colors: {
        'editor.background': '#1E1E1E',
        'editor.foreground': '#D4D4D4',
        'editorLineNumber.foreground': '#858585',
        'editor.selectionBackground': '#264F78',
        'editor.inactiveSelectionBackground': '#3A3D41',
      },
    })

    monaco.editor.setTheme('rego-theme')

    // Configure editor options
    editor.updateOptions({
      minimap: { enabled: true },
      fontSize: 14,
      lineNumbers: 'on',
      renderWhitespace: 'selection',
      scrollBeyondLastLine: false,
      automaticLayout: true,
      tabSize: 2,
      insertSpaces: true,
    })
  }

  /**
   * Handle editor value change
   */
  const handleEditorChange = (value: string | undefined) => {
    onChange(value || '')
  }

  useEffect(() => {
    if (editorRef.current) {
      editorRef.current.updateOptions({ readOnly })
    }
  }, [readOnly])

  return (
    <div className="rounded-md border">
      <Editor
        height={height}
        language="rego"
        value={value}
        onChange={handleEditorChange}
        onMount={handleEditorDidMount}
        options={{
          readOnly,
          selectOnLineNumbers: true,
          roundedSelection: false,
          cursorStyle: 'line',
          automaticLayout: true,
        }}
      />
    </div>
  )
}
