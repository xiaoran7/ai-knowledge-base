<template>
  <div class="graph-shell">
    <div class="graph-toolbar">
      <div class="graph-toolbar__legend">
        <span class="graph-chip graph-chip--doc">文档节点</span>
        <span class="graph-chip graph-chip--cat">分类节点</span>
        <span class="graph-chip graph-chip--ref">引用链接</span>
        <span class="graph-chip graph-chip--tag">共享标签</span>
      </div>
      <div class="graph-toolbar__actions">
        <label class="graph-zoom">
          <span>缩放</span>
          <input v-model="zoom" type="range" min="70" max="140" step="5" />
          <strong>{{ zoom }}%</strong>
        </label>
        <button class="graph-btn" @click="relayout">重新布局</button>
      </div>
    </div>

    <div v-if="!graph.nodes.length" class="graph-empty">
      <h3>这个知识库还没有可展示的网络</h3>
      <p>先上传文档、给文档分类，或者在文档正文和摘要里出现其他文档标题/`[[文档名]]` 链接，网络就会逐步长出来。</p>
    </div>

    <div v-else class="graph-layout">
      <div class="graph-stage">
        <svg
          class="graph-svg"
          viewBox="0 0 1200 760"
          preserveAspectRatio="xMidYMid meet"
        >
          <g :transform="`scale(${zoom / 100}) translate(${translateX}, ${translateY})`">
            <line
              v-for="edge in visibleEdges"
              :key="edge.id"
              :x1="positionMap[edge.source]?.x || 0"
              :y1="positionMap[edge.source]?.y || 0"
              :x2="positionMap[edge.target]?.x || 0"
              :y2="positionMap[edge.target]?.y || 0"
              :stroke="edgeColor(edge.type)"
              :stroke-width="edgeWidth(edge)"
              :stroke-opacity="edgeOpacity(edge)"
            />

            <g
              v-for="node in graph.nodes"
              :key="node.id"
              class="graph-node"
              :class="{ 'is-active': selectedNode?.id === node.id }"
              :transform="`translate(${positionMap[node.id]?.x || 0}, ${positionMap[node.id]?.y || 0})`"
              @click="selectNode(node.id)"
            >
              <circle
                :r="nodeRadius(node)"
                :fill="nodeFill(node)"
                :stroke="selectedNode?.id === node.id ? '#0f172a' : 'rgba(15, 23, 42, 0.12)'"
                :stroke-width="selectedNode?.id === node.id ? 3 : 1.5"
              />
              <text
                class="graph-node__label"
                :y="node.type === 'CATEGORY' ? nodeRadius(node) + 22 : nodeRadius(node) + 20"
                text-anchor="middle"
              >
                {{ shorten(node.title, node.type === 'CATEGORY' ? 10 : 14) }}
              </text>
            </g>
          </g>
        </svg>
      </div>

      <aside class="graph-side">
        <div class="graph-side__stats">
          <div class="graph-stat">
            <span>节点</span>
            <strong>{{ graph.stats.totalNodes }}</strong>
          </div>
          <div class="graph-stat">
            <span>关系</span>
            <strong>{{ graph.stats.totalEdges }}</strong>
          </div>
          <div class="graph-stat">
            <span>引用边</span>
            <strong>{{ graph.stats.referenceEdges }}</strong>
          </div>
          <div class="graph-stat">
            <span>孤立文档</span>
            <strong>{{ graph.stats.orphanDocuments }}</strong>
          </div>
        </div>

        <div v-if="selectedNode" class="graph-side__card">
          <div class="graph-side__header">
            <span class="graph-side__badge" :class="selectedNode.type === 'CATEGORY' ? 'is-category' : 'is-document'">
              {{ selectedNode.type === 'CATEGORY' ? '分类' : '文档' }}
            </span>
            <h3>{{ selectedNode.title }}</h3>
          </div>

          <div class="graph-side__meta">
            <div class="graph-meta-line">
              <span>连接度</span>
              <strong>{{ selectedNode.degree }}</strong>
            </div>
            <div class="graph-meta-line">
              <span>入链</span>
              <strong>{{ selectedNode.inbound }}</strong>
            </div>
            <div class="graph-meta-line">
              <span>出链</span>
              <strong>{{ selectedNode.outbound }}</strong>
            </div>
            <div v-if="selectedNode.categoryName" class="graph-meta-line">
              <span>分类</span>
              <strong>{{ selectedNode.categoryName }}</strong>
            </div>
            <div v-if="selectedNode.summaryType" class="graph-meta-line">
              <span>总结方式</span>
              <strong>{{ selectedNode.summaryType }}</strong>
            </div>
          </div>

          <div v-if="selectedNode.tags?.length" class="graph-tags">
            <span v-for="tag in selectedNode.tags" :key="tag" class="graph-tag">{{ tag }}</span>
          </div>

          <div class="graph-relations">
            <div class="graph-relations__title">出链</div>
            <button
              v-for="item in outgoingRelations"
              :key="`out-${item.node.id}-${item.edge.id}`"
              class="graph-relation"
              @click="selectNode(item.node.id)"
            >
              <div>
                <h4>{{ item.node.title }}</h4>
                <p>{{ relationText(item.edge.type, item.edge.label) }}</p>
              </div>
              <span>{{ item.node.degree }}</span>
            </button>
            <p v-if="!outgoingRelations.length" class="graph-relations__empty">当前节点没有出链。</p>
          </div>

          <div class="graph-relations">
            <div class="graph-relations__title">反向链接</div>
            <button
              v-for="item in incomingRelations"
              :key="`in-${item.node.id}-${item.edge.id}`"
              class="graph-relation graph-relation--incoming"
              @click="selectNode(item.node.id)"
            >
              <div>
                <h4>{{ item.node.title }}</h4>
                <p>{{ relationText(item.edge.type, item.edge.label) }}</p>
              </div>
              <span>{{ item.node.degree }}</span>
            </button>
            <p v-if="!incomingRelations.length" class="graph-relations__empty">当前节点还没有反向链接。</p>
          </div>

          <div class="graph-relations">
            <div class="graph-relations__title">全部关联</div>
            <button
              v-for="item in relatedNodes"
              :key="item.node.id + item.edge.id"
              class="graph-relation"
              @click="selectNode(item.node.id)"
            >
              <div>
                <h4>{{ item.node.title }}</h4>
                <p>{{ relationText(item.edge.type, item.edge.label) }}</p>
              </div>
              <span>{{ item.node.degree }}</span>
            </button>
            <p v-if="!relatedNodes.length" class="graph-relations__empty">当前节点暂时没有更多关联。</p>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { KnowledgeGraphEdge, KnowledgeGraphNode, KnowledgeGraphResponse } from '@/api/knowledge'

type Position = { x: number; y: number }

const props = defineProps<{
  graph: KnowledgeGraphResponse
}>()

const positionMap = reactive<Record<string, Position>>({})
const selectedNodeId = ref<string>('')
const zoom = ref(100)
const translateX = ref(0)
const translateY = ref(0)

const selectedNode = computed(() => props.graph.nodes.find((node) => node.id === selectedNodeId.value) || null)

const visibleEdges = computed(() => props.graph.edges.filter((edge) => positionMap[edge.source] && positionMap[edge.target]))

const relatedNodes = computed(() => {
  if (!selectedNode.value) return []
  return props.graph.edges
    .filter((edge) => edge.source === selectedNode.value?.id || edge.target === selectedNode.value?.id)
    .map((edge) => {
      const otherId = edge.source === selectedNode.value?.id ? edge.target : edge.source
      return {
        edge,
        node: props.graph.nodes.find((node) => node.id === otherId)!
      }
    })
    .filter((item) => item.node)
    .sort((left, right) => right.node.degree - left.node.degree)
})

const outgoingRelations = computed(() => {
  if (!selectedNode.value) return []
  return props.graph.edges
    .filter((edge) => edge.source === selectedNode.value?.id)
    .map((edge) => ({
      edge,
      node: props.graph.nodes.find((node) => node.id === edge.target)!
    }))
    .filter((item) => item.node)
    .sort((left, right) => right.node.degree - left.node.degree)
})

const incomingRelations = computed(() => {
  if (!selectedNode.value) return []
  return props.graph.edges
    .filter((edge) => edge.target === selectedNode.value?.id)
    .map((edge) => ({
      edge,
      node: props.graph.nodes.find((node) => node.id === edge.source)!
    }))
    .filter((item) => item.node)
    .sort((left, right) => right.node.degree - left.node.degree)
})

watch(
  () => props.graph,
  (graph) => {
    relayout()
    if (!selectedNodeId.value && graph.nodes.length) {
      selectedNodeId.value = graph.nodes.find((node) => node.type === 'DOCUMENT')?.id || graph.nodes[0].id
    } else if (selectedNodeId.value && !graph.nodes.some((node) => node.id === selectedNodeId.value)) {
      selectedNodeId.value = graph.nodes[0]?.id || ''
    }
  },
  { immediate: true, deep: true }
)

function relayout() {
  const width = 1200
  const height = 760
  const centerX = width / 2
  const centerY = height / 2
  const categoryNodes = props.graph.nodes.filter((node) => node.type === 'CATEGORY')
  const documentNodes = props.graph.nodes.filter((node) => node.type === 'DOCUMENT')

  const positions = new Map<string, Position>()

  categoryNodes.forEach((node, index) => {
    const angle = (index / Math.max(1, categoryNodes.length)) * Math.PI * 2
    positions.set(node.id, {
      x: centerX + Math.cos(angle) * 165,
      y: centerY + Math.sin(angle) * 145
    })
  })

  documentNodes.forEach((node, index) => {
    const angle = (index / Math.max(1, documentNodes.length)) * Math.PI * 2
    const radius = 250 + (index % 5) * 18
    positions.set(node.id, {
      x: centerX + Math.cos(angle) * radius,
      y: centerY + Math.sin(angle) * (radius * 0.74)
    })
  })

  const simulationNodes = props.graph.nodes.map((node) => ({
    id: node.id,
    type: node.type,
    x: positions.get(node.id)?.x || centerX,
    y: positions.get(node.id)?.y || centerY,
    vx: 0,
    vy: 0
  }))

  for (let iteration = 0; iteration < 220; iteration++) {
    for (let i = 0; i < simulationNodes.length; i++) {
      for (let j = i + 1; j < simulationNodes.length; j++) {
        const a = simulationNodes[i]
        const b = simulationNodes[j]
        let dx = b.x - a.x
        let dy = b.y - a.y
        let distance = Math.sqrt(dx * dx + dy * dy) || 1
        const force = 2100 / (distance * distance)
        dx /= distance
        dy /= distance
        a.vx -= dx * force
        a.vy -= dy * force
        b.vx += dx * force
        b.vy += dy * force
      }
    }

    for (const edge of props.graph.edges) {
      const source = simulationNodes.find((node) => node.id === edge.source)
      const target = simulationNodes.find((node) => node.id === edge.target)
      if (!source || !target) continue

      const idealLength = edge.type === 'CATEGORY_MEMBERSHIP' ? 110 : edge.type === 'CATEGORY_TREE' ? 90 : 160
      const strength = edge.type === 'REFERENCE' ? 0.02 : edge.type === 'SHARED_TAG' ? 0.014 : 0.03
      const dx = target.x - source.x
      const dy = target.y - source.y
      const distance = Math.sqrt(dx * dx + dy * dy) || 1
      const pull = (distance - idealLength) * strength
      const nx = dx / distance
      const ny = dy / distance
      source.vx += nx * pull
      source.vy += ny * pull
      target.vx -= nx * pull
      target.vy -= ny * pull
    }

    for (const node of simulationNodes) {
      const toCenterX = centerX - node.x
      const toCenterY = centerY - node.y
      const centerStrength = node.type === 'CATEGORY' ? 0.006 : 0.0026
      node.vx += toCenterX * centerStrength
      node.vy += toCenterY * centerStrength

      node.vx *= 0.82
      node.vy *= 0.82
      node.x = clamp(node.x + node.vx, 80, width - 80)
      node.y = clamp(node.y + node.vy, 80, height - 80)
    }
  }

  Object.keys(positionMap).forEach((key) => delete positionMap[key])
  simulationNodes.forEach((node) => {
    positionMap[node.id] = { x: node.x, y: node.y }
  })
}

function selectNode(nodeId: string) {
  selectedNodeId.value = nodeId
}

function nodeRadius(node: KnowledgeGraphNode) {
  if (node.type === 'CATEGORY') return node.virtualNode ? 18 : 22
  return Math.min(26, 16 + Math.max(0, node.degree - 1) * 0.7)
}

function nodeFill(node: KnowledgeGraphNode) {
  if (node.type === 'CATEGORY') {
    return node.virtualNode ? '#e2e8f0' : '#dbeafe'
  }
  if (node.status === 'FAILED') return '#fee2e2'
  if (node.status === 'PROCESSING') return '#fef3c7'
  return '#dcfce7'
}

function edgeColor(type: string) {
  if (type === 'REFERENCE') return '#2563eb'
  if (type === 'SHARED_TAG') return '#10b981'
  if (type === 'CATEGORY_TREE') return '#94a3b8'
  return '#cbd5e1'
}

function edgeWidth(edge: KnowledgeGraphEdge) {
  return edge.type === 'REFERENCE' ? 2.2 : Math.min(2.4, 1 + edge.weight * 0.45)
}

function edgeOpacity(edge: KnowledgeGraphEdge) {
  if (edge.type === 'REFERENCE') return 0.78
  if (edge.type === 'SHARED_TAG') return 0.62
  return 0.46
}

function relationText(type: string, label?: string) {
  if (label) return label
  if (type === 'REFERENCE') return '文档引用'
  if (type === 'SHARED_TAG') return '共享标签'
  if (type === 'CATEGORY_MEMBERSHIP') return '同一分类'
  if (type === 'CATEGORY_TREE') return '分类层级'
  return type
}

function shorten(text: string, max: number) {
  return text.length > max ? `${text.slice(0, max)}…` : text
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}
</script>

<style scoped>
.graph-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.graph-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #f8fafc;
  padding: 14px 16px;
}

.graph-toolbar__legend,
.graph-toolbar__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.graph-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 600;
}

.graph-chip--doc {
  background: #dcfce7;
  color: #166534;
}

.graph-chip--cat {
  background: #dbeafe;
  color: #1d4ed8;
}

.graph-chip--ref {
  background: #dbeafe;
  color: #2563eb;
}

.graph-chip--tag {
  background: #d1fae5;
  color: #047857;
}

.graph-zoom {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #475569;
}

.graph-btn {
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background: white;
  padding: 9px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.graph-empty {
  border: 1px dashed #cbd5e1;
  border-radius: 22px;
  background: #f8fafc;
  padding: 44px 24px;
  text-align: center;
}

.graph-empty h3 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.graph-empty p {
  margin: 12px auto 0;
  max-width: 700px;
  line-height: 1.8;
  color: #64748b;
}

.graph-layout {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) 320px;
}

.graph-stage,
.graph-side__card,
.graph-side__stats {
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  background: white;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.04);
}

.graph-stage {
  overflow: hidden;
  min-height: 760px;
  background:
    radial-gradient(circle at top left, rgba(96, 165, 250, 0.08), transparent 26%),
    radial-gradient(circle at bottom right, rgba(34, 197, 94, 0.08), transparent 22%),
    #ffffff;
}

.graph-svg {
  display: block;
  width: 100%;
  height: 760px;
}

.graph-node {
  cursor: pointer;
}

.graph-node__label {
  fill: #334155;
  font-size: 12px;
  font-weight: 600;
}

.graph-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.graph-side__stats {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding: 16px;
}

.graph-stat {
  border-radius: 18px;
  background: #f8fafc;
  padding: 14px;
}

.graph-stat span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.graph-stat strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
  color: #0f172a;
}

.graph-side__card {
  padding: 18px;
}

.graph-side__header h3 {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.45;
}

.graph-side__badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
}

.graph-side__badge.is-document {
  background: #dcfce7;
  color: #166534;
}

.graph-side__badge.is-category {
  background: #dbeafe;
  color: #1d4ed8;
}

.graph-side__meta {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.graph-meta-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 14px;
  background: #f8fafc;
  padding: 10px 12px;
  font-size: 13px;
  color: #475569;
}

.graph-meta-line strong {
  color: #0f172a;
}

.graph-tags {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.graph-tag {
  border-radius: 999px;
  background: #eff6ff;
  padding: 5px 10px;
  font-size: 12px;
  color: #2563eb;
}

.graph-relations {
  margin-top: 18px;
}

.graph-relations__title {
  font-size: 13px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.graph-relation {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: #fff;
  padding: 12px;
  text-align: left;
}

.graph-relation h4 {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.graph-relation p {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.graph-relation span {
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
}

.graph-relation--incoming {
  background: #f8fafc;
}

.graph-relations__empty {
  margin-top: 12px;
  font-size: 13px;
  color: #94a3b8;
}

@media (max-width: 1200px) {
  .graph-layout {
    grid-template-columns: 1fr;
  }

  .graph-stage {
    min-height: 620px;
  }

  .graph-svg {
    height: 620px;
  }
}
</style>
