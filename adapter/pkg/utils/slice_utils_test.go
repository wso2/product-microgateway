/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package utils

import "testing"

func TestChunkSlice(t *testing.T) {
	slice := []string{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"}
	chunkSize := 3
	expectedChunks := [][]string{{"a", "b", "c"}, {"d", "e", "f"}, {"g", "h", "i"}, {"j"}}
	chunks := ChunkSlice(slice, chunkSize)
	if len(chunks) != len(expectedChunks) {
		t.Errorf("Expected chunks length: %d, but got: %d", len(expectedChunks), len(chunks))
	}
	for i, chunk := range chunks {
		if len(chunk) != len(expectedChunks[i]) {
			t.Errorf("Expected chunk length: %d, but got: %d", len(expectedChunks[i]), len(chunk))
		}
		for j, val := range chunk {
			if val != expectedChunks[i][j] {
				t.Errorf("Expected chunk value: %s, but got: %s", expectedChunks[i][j], val)
			}
		}
	}
}
