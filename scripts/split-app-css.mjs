import fs from 'node:fs'
import path from 'node:path'
import postcss from 'postcss'

const repoRoot = process.cwd()
const appCssPath = path.join(repoRoot, 'apps', 'web', 'src', 'App.css')
const stylesDir = path.join(repoRoot, 'apps', 'web', 'src', 'styles')

const chunkCount = 5

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function writeFile(filePath, content) {
  fs.writeFileSync(filePath, content.trimEnd() + '\n', 'utf8')
}

function main() {
  const raw = fs.readFileSync(appCssPath, 'utf8')
  let root = postcss.parse(raw)
  const allNodes = root.nodes || []
  const importNodes = []
  const nodes = []

  for (const node of allNodes) {
    if (node.type === 'atrule' && node.name === 'import') {
      const normalizedImport = node.toString().trim().replace(/;?$/, ';')
      importNodes.push(normalizedImport)
      continue
    }
    nodes.push(node)
  }

  if (nodes.length === 0 && importNodes.length > 0) {
    const importedCss = importNodes
      .map((statement) => {
        const match = statement.match(/['"](.+?)['"]/)
        if (!match) return ''
        const importPath = match[1]
        const absPath = path.resolve(path.dirname(appCssPath), importPath)
        if (!fs.existsSync(absPath)) return ''
        return fs.readFileSync(absPath, 'utf8')
      })
      .filter(Boolean)
      .join('\n\n')
    if (!importedCss.trim()) {
      console.log('SPLIT_APP_CSS_SKIPPED already-split')
      return
    }
    root = postcss.parse(importedCss)
    nodes.push(...(root.nodes || []))
    importNodes.length = 0
  }

  if (nodes.length === 0) {
    throw new Error('App.css has no nodes to split')
  }

  const nodeTexts = nodes.map((node) => node.toString())
  const totalSize = nodeTexts.reduce((sum, text) => sum + text.length, 0)
  const targetSize = Math.ceil(totalSize / chunkCount)

  const chunks = []
  let current = []
  let currentSize = 0

  for (let i = 0; i < nodeTexts.length; i += 1) {
    const text = nodeTexts[i]
    const isLastChunk = chunks.length === chunkCount - 1

    if (!isLastChunk && current.length > 0 && currentSize + text.length > targetSize) {
      chunks.push(current)
      current = []
      currentSize = 0
    }

    current.push(text)
    currentSize += text.length
  }

  if (current.length > 0) {
    chunks.push(current)
  }

  while (chunks.length < chunkCount) {
    chunks.push([])
  }

  ensureDir(stylesDir)

  const imports = [...importNodes]
  for (let i = 0; i < chunkCount; i += 1) {
    const fileName = `app-core-${i + 1}.css`
    const filePath = path.join(stylesDir, fileName)
    const content = chunks[i].join('\n\n')
    writeFile(filePath, content)
    imports.push(`@import './styles/${fileName}';`)
  }

  writeFile(appCssPath, imports.join('\n'))

  const sizes = chunks.map((chunk) => chunk.join('\n\n').length)
  console.log(`SPLIT_APP_CSS_OK chunks=${chunkCount} sizes=${sizes.join(',')}`)
}

main()
