/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.View;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.Test;

class AbstractServiceTest {

  @Test
  void constructorDiscardsEmptyName() {
    assertThrows(IllegalArgumentException.class,
        () -> new ServiceUnderTest((short) 1, "", mock(TransactionConverter.class)));
  }

  @Test
  void constructorDiscardsNullName() {
    assertThrows(NullPointerException.class,
        () -> new ServiceUnderTest((short) 1, null, mock(TransactionConverter.class)));
  }

  @Test
  void constructorDiscardsNullConverter() {
    assertThrows(NullPointerException.class,
        () -> new ServiceUnderTest((short) 1, "service#1", null));
  }

  @Test
  void getStateHashes_EmptySchema() {
    Service service = new ServiceUnderTest((short) 1, "s1", mock(TransactionConverter.class));
    assertTrue(service.getStateHashes(mock(Snapshot.class)).isEmpty());
  }

  static class ServiceUnderTest extends AbstractService {

    ServiceUnderTest(short id, String name,
        TransactionConverter transactionConverter) {
      super(id, name, transactionConverter);
    }

    @Override
    protected Schema createDataSchema(View view) {
      return mock(Schema.class);
    }

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
    }
  }
}
