import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client from '../api/client.js';

// 재귀 트리 노드. visited 로 순환 방지.
function TreeNode({ id, nodeMap, adjacency, visited, rootId }) {
  const node = nodeMap.get(id);
  if (!node) return null;
  const children = (adjacency.get(id) || []).filter((c) => !visited.has(c));
  const nextVisited = new Set(visited);
  children.forEach((c) => nextVisited.add(c));

  return (
    <li className="tree-node">
      <span className="tree-label">
        {id === rootId
          ? <strong>{node.title}</strong>
          : <Link to={`/papers/${id}`}>{node.title}</Link>}
        <span className="tree-cite muted small"> · 피인용 {node.citationCount}</span>
      </span>
      {children.length > 0 && (
        <ul className="tree-children">
          {children.map((c) => (
            <TreeNode key={c} id={c} nodeMap={nodeMap} adjacency={adjacency} visited={nextVisited} rootId={rootId} />
          ))}
        </ul>
      )}
    </li>
  );
}

export default function CitationTree({ paperId }) {
  const [graph, setGraph] = useState(null);

  useEffect(() => {
    client.get(`/papers/${paperId}/citation-graph`)
      .then(({ data }) => setGraph(data))
      .catch(() => setGraph({ root: Number(paperId), nodes: [], edges: [] }));
  }, [paperId]);

  if (!graph) return <p className="muted">인용 관계 불러오는 중…</p>;

  const nodeMap = new Map(graph.nodes.map((n) => [n.id, n]));
  // references: from -> [to] (이 논문이 인용한 논문)
  const outAdj = new Map();
  // citedBy: to -> [from] (이 논문을 인용한 논문)
  const inAdj = new Map();
  for (const e of graph.edges) {
    if (!outAdj.has(e.from)) outAdj.set(e.from, []);
    outAdj.get(e.from).push(e.to);
    if (!inAdj.has(e.to)) inAdj.set(e.to, []);
    inAdj.get(e.to).push(e.from);
  }

  const root = graph.root;
  const hasRefs = (outAdj.get(root) || []).length > 0;
  const hasCitedBy = (inAdj.get(root) || []).length > 0;

  if (!hasRefs && !hasCitedBy) {
    return <p className="muted">이 논문과 연결된 저널 내 인용 관계가 없습니다.</p>;
  }

  return (
    <div className="citation-tree">
      <div className="tree-col">
        <h4>참고문헌 <span className="muted small">(이 논문이 인용)</span></h4>
        {hasRefs ? (
          <ul className="tree-root">
            <TreeNode id={root} nodeMap={nodeMap} adjacency={outAdj} visited={new Set([root])} rootId={root} />
          </ul>
        ) : <p className="muted small">없음</p>}
      </div>
      <div className="tree-col">
        <h4>피인용 <span className="muted small">(이 논문을 인용)</span></h4>
        {hasCitedBy ? (
          <ul className="tree-root">
            <TreeNode id={root} nodeMap={nodeMap} adjacency={inAdj} visited={new Set([root])} rootId={root} />
          </ul>
        ) : <p className="muted small">없음</p>}
      </div>
    </div>
  );
}
