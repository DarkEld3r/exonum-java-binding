/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.proofs.list;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/**
 * A validator that checks list proofs internal structure.
 */
final class ListProofStructureValidator implements ListProofVisitor {

  static final int MAX_NODE_DEPTH = 63;

  // TODO: optimize data storage and algorithms of validity checks, https://jira.bf.local/browse/ECR-2443
  private final Multimap<NodeType, NodeInfo> listProofInfo;

  private int depth;

  private ListProofStatus proofStatus;

  /**
   * Creates a new ListProofStructureValidator.
   */
  ListProofStructureValidator(ListProofNode listProof) {
    depth = 0;
    proofStatus = ListProofStatus.VALID;
    listProofInfo = Multimaps.newListMultimap(new EnumMap<>(NodeType.class), ArrayList::new);

    listProof.accept(this);
    this.check();
  }

  @Override
  public void visit(ListProofBranch branch) {
    int branchDepth = depth;

    NodeType leftElementType = getNodeType(branch.getLeft());
    NodeType rightElementType = branch.getRight().map(this::getNodeType).orElse(NodeType.NONE);
    listProofInfo.get(NodeType.BRANCH).add(
        new NodeInfo(branch, depth, Arrays.asList(leftElementType, rightElementType))
    );

    visitLeft(branch, branchDepth);
    visitRight(branch, branchDepth);
  }

  @Override
  public void visit(ListProofHashNode hashNode) {
    listProofInfo.get(NodeType.HASHNODE).add(new NodeInfo(hashNode, depth));
  }

  @Override
  public void visit(ListProofElement element) {
    listProofInfo.get(NodeType.ELEMENT).add(new NodeInfo(element, depth));
  }

  @Override
  public void visit(ListProofOfAbsence listProofOfAbsence) {
    listProofInfo.get(NodeType.ABSENCE).add(new NodeInfo(listProofOfAbsence, depth));
  }

  private void visitLeft(ListProofBranch branch, int branchDepth) {
    depth = getChildDepth(branchDepth);
    branch.getLeft().accept(this);
  }

  private void visitRight(ListProofBranch branch, int branchDepth) {
    depth = getChildDepth(branchDepth);
    branch.getRight().ifPresent((right) -> right.accept(this));
  }

  private int getChildDepth(int branchDepth) {
    return branchDepth + 1;
  }

  /**
   * Performs list proof structure checks and assigns proofStatus based on results of these checks.
   */
  private void check() {
    if (exceedsMaxDepth(listProofInfo.get(NodeType.ELEMENT))) {
      proofStatus = ListProofStatus.INVALID_ELEMENT_NODE_DEPTH;
    } else if (exceedsMaxDepth(listProofInfo.get(NodeType.HASHNODE))) {
      proofStatus = ListProofStatus.INVALID_HASH_NODE_DEPTH;
    } else if (hasInvalidNodesDepth(listProofInfo.get(NodeType.ELEMENT))) {
      proofStatus = ListProofStatus.INVALID_NODE_DEPTH;
    } else if (hasNoElementNodes(listProofInfo.get(NodeType.BRANCH),
        listProofInfo.get(NodeType.ELEMENT))) {
      proofStatus = ListProofStatus.INVALID_TREE_NO_ELEMENTS;
    } else if (hashNodesLimitExceeded(listProofInfo.get(NodeType.BRANCH))) {
      proofStatus = ListProofStatus.INVALID_HASH_NODES_COUNT;
    } else if (hasInvalidAbsentNodes()) {
      proofStatus = ListProofStatus.INVALID_PROOF_OF_ABSENCE;
    }
  }

  /**
   * Returns tree node type based on node class type.
   *
   * @throws RuntimeException in case if node type is unknown
   */
  private NodeType getNodeType(ListProofNode node) {
    if (node instanceof ListProofBranch) {
      return NodeType.BRANCH;
    } else if (node instanceof ListProofElement) {
      return NodeType.ELEMENT;
    } else if (node instanceof ListProofHashNode) {
      return NodeType.HASHNODE;
    } else if (node instanceof ListProofOfAbsence) {
      return NodeType.ABSENCE;
    } else {
      throw new RuntimeException("Unknown tree node type: " + node);
    }
  }

  /**
   * Returns true if node exceeds the maximum depth at which nodes may appear.
   *
   * @param nodes collection of node info
   * @return true if node depth is invalid
   */
  private boolean exceedsMaxDepth(Collection<NodeInfo> nodes) {
    return nodes.stream()
        .anyMatch(nodeInfo -> nodeInfo.getDepth() > MAX_NODE_DEPTH);
  }

  /**
   * Returns true if nodes appear at different depths.
   *
   * @param nodes collection of node info
   * @return true if node depths vary
   */
  private boolean hasInvalidNodesDepth(Collection<NodeInfo> nodes) {
    long depthsCount = nodes.stream()
        .map(NodeInfo::getDepth)
        .distinct()
        .count();
    return depthsCount > 1;
  }

  /**
   * Returns true if tree doesn't contain listProofElement nodes.
   *
   * @param branches collection of branches info
   * @param nodes collection of node info
   * @return true if node depths vary
   */
  private boolean hasNoElementNodes(Collection<NodeInfo> branches, Collection<NodeInfo> nodes) {
    return branches.size() > 0 && nodes.size() == 0;
  }

  /**
   * Returns true if child nodes of the branch node are not valid: contain hashes only.
   *
   * @param branches collection of branches info
   * @return true if branch contains only hash nodes.
   */
  private boolean hashNodesLimitExceeded(Collection<NodeInfo> branches) {
    return branches.stream()
        .anyMatch(this::invalidBranch);
  }

  private boolean invalidBranch(NodeInfo branch) {
    List<NodeType> children = branch.getChildElementsTypes();
    if (children.size() < 1) {
      return true;
    }
    return children.stream()
        .allMatch(nodeType -> (nodeType == NodeType.HASHNODE) || (nodeType == NodeType.NONE));
  }

  /**
   * Returns true if this proof is a proof of absence and doesn't have a valid structure:
   * contains more than one node of absence, or contains any nodes other than a node of absence,
   * or the node of absence has non-zero depth.
   */
  private boolean hasInvalidAbsentNodes() {
    Collection<NodeInfo> absentNodes = listProofInfo.get(NodeType.ABSENCE);
    int absentNodesSize = absentNodes.size();
    if (absentNodesSize > 1) {
      return true;
    } else if (absentNodesSize == 1) {
      boolean singleNodeTree = listProofInfo.get(NodeType.BRANCH).isEmpty()
          && listProofInfo.get(NodeType.HASHNODE).isEmpty()
          && listProofInfo.get(NodeType.ELEMENT).isEmpty();
      return !singleNodeTree || absentNodes.stream().allMatch(node -> node.getDepth() != 0);
    } else {
      return false;
    }
  }

  /**
   * Returns proof status.
   */
  ListProofStatus getProofStatus() {
    return proofStatus;
  }

  /**
   * Returns true if proof status is VALID, false otherwise.
   */
  boolean isValid() {
    return proofStatus == ListProofStatus.VALID;
  }

  @Override
  public String toString() {
    return "ListProofStructureValidator{"
        + ", proofStatus" + proofStatus
        + ", depth=" + depth
        + '}';
  }

  /**
   * Class used to store node info additional information.
   */
  private static class NodeInfo {
    private final ListProofNode node;
    private final int depth;
    private final List<NodeType> childElementsTypes;

    NodeInfo(ListProofNode node, int depth) {
      this(node, depth, Collections.emptyList());
    }

    NodeInfo(ListProofNode node, int depth, List<NodeType> childElementsTypes) {
      this.node = node;
      this.depth = depth;
      this.childElementsTypes = childElementsTypes;
    }

    ListProofNode getNode() {
      return node;
    }

    int getDepth() {
      return depth;
    }

    List<NodeType> getChildElementsTypes() {
      return childElementsTypes;
    }
  }

  /**
   * Enum used to identify tree node type.
   */
  private enum NodeType {
    BRANCH, ELEMENT, HASHNODE, ABSENCE, NONE
  }
}
