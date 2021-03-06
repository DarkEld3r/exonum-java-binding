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

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.hash.HashCode;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

class UncheckedListProofAdapterTest {

  private static final ByteString V1 = ByteString.copyFromUtf8("v1");

  private static final HashCode H1 = HashCode.fromString("a1");
  private static final HashCode H2 = HashCode.fromString("a2");

  @Test
  void check_validProof() {
    ListProofNode left = new ListProofElement(V1);
    ListProofNode right = new ListProofHashNode(H1);
    ListProofBranch root = new ListProofBranch(left, right);

    long length = 2;
    UncheckedListProofAdapter uncheckedProof = createUncheckedProof(root, length);

    CheckedListProof checkedProof = uncheckedProof.check();

    assertThat(uncheckedProof.getListProofRootNode(), equalTo(root));
    assertThat(checkedProof.getProofStatus(), is(ListProofStatus.VALID));
    assertThat(checkedProof.getElements(), equalTo(of(0L, V1)));
    assertThat(checkedProof.getIndexHash(), notNullValue());
  }

  @Test
  void check_invalidProof() {
    ListProofNode left = new ListProofHashNode(H1);
    ListProofNode right = new ListProofHashNode(H2);
    ListProofBranch root = new ListProofBranch(left, right);

    long length = 2;
    UncheckedListProofAdapter uncheckedProof = createUncheckedProof(root, length);

    CheckedListProof checkedProof = uncheckedProof.check();

    assertThat(checkedProof.getProofStatus(), is(ListProofStatus.INVALID_TREE_NO_ELEMENTS));

    assertThrows(IllegalStateException.class, checkedProof::getElements);
    assertThrows(IllegalStateException.class, checkedProof::getIndexHash);
  }

  private UncheckedListProofAdapter createUncheckedProof(ListProofBranch root, long length) {
    return new UncheckedListProofAdapter(root, length);
  }
}
