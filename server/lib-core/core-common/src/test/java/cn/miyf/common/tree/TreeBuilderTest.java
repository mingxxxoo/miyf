package cn.miyf.common.tree;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 无限极树构建单测。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
class TreeBuilderTest {

    @Test
    void buildUnlimitedDepthWithParentCodeAndSort() {
        List<Node> flat = List.of(
                node("a", null, 2),
                node("a1", "a", 20),
                node("a2", "a", 10),
                node("a2x", "a2", 1),
                node("a2y", "a2", 2),
                node("b", null, 1)
        );

        List<Node> roots = TreeUtils.builder(Node::getCode, Node::getParentCode, Node::getChildren, Node::setChildren)
                .rootWhen(Objects::isNull)
                .sortBy(Comparator.comparing(Node::getSort, Comparator.nullsLast(Integer::compareTo)))
                .build(copy(flat));

        assertEquals(List.of("b", "a"), roots.stream().map(Node::getCode).toList());
        Node a = roots.get(1);
        assertEquals(List.of("a2", "a1"), a.getChildren().stream().map(Node::getCode).toList());
        Node a2 = a.getChildren().get(0);
        assertEquals(List.of("a2x", "a2y"), a2.getChildren().stream().map(Node::getCode).toList());
        // 第三层仍可继续挂载，证明无限极
        assertEquals(0, a2.getChildren().get(0).getChildren().size());
    }

    @Test
    void buildMappedAndFlatten() {
        List<Src> sources = List.of(
                new Src(1L, null, "r"),
                new Src(2L, 1L, "c"),
                new Src(3L, 2L, "g")
        );
        TreeBuilder<Node, Long> builder = TreeBuilder.of(
                Node::getId, Node::getParentId, Node::getChildren, Node::setChildren);
        List<Node> roots = builder.buildMapped(sources, s -> {
            Node n = new Node();
            n.setId(s.id());
            n.setParentId(s.parentId());
            n.setCode(s.code());
            n.setChildren(new ArrayList<>());
            return n;
        });
        assertEquals(1, roots.size());
        assertEquals(List.of("r", "c", "g"),
                builder.flatten(roots).stream().map(Node::getCode).toList());
    }

    @Test
    void orphanAsRootFalseDropsMissingParent() {
        List<Node> flat = List.of(node("x", "missing", 1));
        List<Node> roots = TreeUtils.builder(Node::getCode, Node::getParentCode, Node::getChildren, Node::setChildren)
                .orphanAsRoot(false)
                .build(copy(flat));
        assertTrue(roots.isEmpty());
    }

    @Test
    void duplicateIdThrows() {
        List<Node> flat = List.of(node("a", null, 1), node("a", null, 2));
        assertThrows(IllegalArgumentException.class, () ->
                TreeUtils.build(copy(flat), Node::getCode, Node::getParentCode, Node::getChildren, Node::setChildren));
    }

    private static List<Node> copy(List<Node> src) {
        List<Node> out = new ArrayList<>();
        for (Node n : src) {
            Node c = new Node();
            c.setId(n.getId());
            c.setParentId(n.getParentId());
            c.setCode(n.getCode());
            c.setParentCode(n.getParentCode());
            c.setSort(n.getSort());
            c.setChildren(new ArrayList<>());
            out.add(c);
        }
        return out;
    }

    private static Node node(String code, String parentCode, int sort) {
        Node n = new Node();
        n.setCode(code);
        n.setParentCode(parentCode);
        n.setSort(sort);
        n.setChildren(new ArrayList<>());
        return n;
    }

    private record Src(Long id, Long parentId, String code) {
    }

    private static final class Node {
        private Long id;
        private Long parentId;
        private String code;
        private String parentCode;
        private Integer sort;
        private List<Node> children = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getParentCode() {
            return parentCode;
        }

        public void setParentCode(String parentCode) {
            this.parentCode = parentCode;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }

        public List<Node> getChildren() {
            return children;
        }

        public void setChildren(List<Node> children) {
            this.children = children;
        }
    }
}
