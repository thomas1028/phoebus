/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.service.saveandrestore.web.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.UpdateConfigHolder;
import org.phoebus.service.saveandrestore.services.IServices;
import org.phoebus.service.saveandrestore.services.exception.NodeNotFoundException;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Main purpose of the tests in this class is to verify that REST end points are
 * maintained, i.e. that URLs are not changed and that they return the correct
 * data.
 *
 * @author Georg Weiss, European Spallation Source
 *
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({ @ContextConfiguration(classes = { ControllersTestConfig.class}) })
@WebMvcTest(ConfigurationController.class)
@SuppressWarnings("unused")

public class ConfigurationControllerTest {

	@Autowired
	private IServices services;

	@Autowired
	private MockMvc mockMvc;

	private static Node folderFromClient;

	private static Node rootNode;

	private static Node config1;

	private static Node snapshot;
	
	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final String JSON = "application/json";

	@BeforeAll
	public static void setUp() {

		config1 = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId("a")
				.userName("myusername").build();
		
		folderFromClient = Node.builder().name("SomeFolder").userName("myusername").id(11).build();

		snapshot = Node.builder().nodeType(NodeType.SNAPSHOT).nodeType(NodeType.SNAPSHOT).name("name")
				.build();

		rootNode = Node.builder().id(Node.ROOT_NODE_ID).uniqueId("root").name("root").build();
		
	}

	@Test
	public void testGetRootNode() throws Exception {

		when(services.getRootNode()).thenReturn(rootNode);

		MockHttpServletRequestBuilder request = get("/root").contentType(JSON);

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		String s = result.getResponse().getContentAsString();
		// Make sure response contains expected data
		objectMapper.readValue(s, Node.class);

	}

	@Test
	public void testCreateFolder() throws Exception {

		when(services.createNode("p", folderFromClient)).thenReturn(folderFromClient);

		MockHttpServletRequestBuilder request = put("/node/p").contentType(JSON)
				.content(objectMapper.writeValueAsString(folderFromClient));

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		String s = result.getResponse().getContentAsString();
		// Make sure response contains expected data
		objectMapper.readValue(s, Node.class);
	}

	@Test
	public void testCreateFolderNoUsername() throws Exception {

		Node folder = Node.builder().name("SomeFolder").id(11).build();

		MockHttpServletRequestBuilder request = put("/node/p").contentType(JSON)
				.content(objectMapper.writeValueAsString(folder));

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void testCreateFolderParentIdDoesNotExist() throws Exception {

		when(services.createNode("p", folderFromClient))
				.thenThrow(new IllegalArgumentException("Parent folder does not exist"));

		MockHttpServletRequestBuilder request = put("/node/p").contentType(JSON)
				.content(objectMapper.writeValueAsString(folderFromClient));

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void testCreateConfig() throws Exception {

		reset(services);
		
		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("config").uniqueId("hhh")
				.userName("user").build();
		when(services.createNode("p", config)).thenAnswer((Answer<Node>) invocation -> config1);
		
		MockHttpServletRequestBuilder request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
	}
	
	@Test
	public void testCreateNodeBadRequests() throws Exception{
		reset(services);
		
		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("config").uniqueId("hhh")
				.build();
		MockHttpServletRequestBuilder request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));
		mockMvc.perform(request).andExpect(status().isBadRequest());
		
		config = Node.builder().nodeType(NodeType.CONFIGURATION).name("config").uniqueId("hhh")
				.userName("").build();
		
		request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));
		mockMvc.perform(request).andExpect(status().isBadRequest());
		
		config = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId("hhh")
				.userName("valid").build();
		
		request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));
		mockMvc.perform(request).andExpect(status().isBadRequest());
		
		config = Node.builder().nodeType(NodeType.CONFIGURATION).name("").uniqueId("hhh")
				.userName("valid").build();
		
		request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));
		mockMvc.perform(request).andExpect(status().isBadRequest());
	}
	
	@Test
	public void testGetChildNodes() throws Exception{
		reset(services);
		
		when(services.getChildNodes("p")).thenAnswer((Answer<List<Node>>) invocation -> Collections.singletonList(config1));
		
		MockHttpServletRequestBuilder request = get("/node/p/children").contentType(JSON);
		
		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();
		
		// Make sure response contains expected data
		List<Node> childNodes = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
		});
		
		assertEquals(1, childNodes.size());
	}
	
	@Test
	public void testGetChildNodesNonExistingNode() throws Exception{
		reset(services);
		
		when(services.getChildNodes("non-existing")).thenThrow(NodeNotFoundException.class);
		MockHttpServletRequestBuilder request = get("/node/non-existing/children").contentType(JSON);
		
		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void testGetNonExistingConfig() throws Exception {

		when(services.getNode("x")).thenThrow(new NodeNotFoundException("lasdfk"));

		MockHttpServletRequestBuilder request = get("/node/x").contentType(JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void testGetSnapshots() throws Exception {

		when(services.getSnapshots("s")).thenReturn(Collections.singletonList(snapshot));

		MockHttpServletRequestBuilder request = get("/config/s/snapshots").contentType(JSON);

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Node>>() {
		});

		reset(services);
	}

	@Test
	public void testGetSnapshotsForNonExistingConfig() throws Exception {

		when(services.getSnapshots("x")).thenThrow(new NodeNotFoundException("lasdfk"));

		MockHttpServletRequestBuilder request = get("/config/x/snapshots").contentType(JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void testDeleteFolder() throws Exception {
		MockHttpServletRequestBuilder request = delete("/node/a");

		mockMvc.perform(request).andExpect(status().isOk());
	}

	@Test
	public void testDeleteNodes() throws Exception {
		MockHttpServletRequestBuilder request = delete("/node")
				.contentType(JSON)
				.content(objectMapper.writeValueAsString(List.of("a")));
		mockMvc.perform(request).andExpect(status().isOk());
	}

	@Test
	public void testGetFolder() throws Exception {
		when(services.getNode("q")).thenReturn(Node.builder().id(1).uniqueId("q").build());

		MockHttpServletRequestBuilder request = get("/node/q");

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
	}

	@Test
	public void testGetConfiguration() throws Exception {
		
		Mockito.reset(services);
		
		when(services.getNode("a")).thenReturn(Node.builder().build());

		MockHttpServletRequestBuilder request = get("/node/a");

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
	}

	@Test
	public void testGetNonExistingConfiguration() throws Exception {
		Mockito.reset(services);
		when(services.getNode("a")).thenThrow(NodeNotFoundException.class);

		MockHttpServletRequestBuilder request = get("/node/a");

		mockMvc.perform(request).andExpect(status().isNotFound());

		
	}

	@Test
	public void testGetNonExistingFolder() throws Exception {
		
		Mockito.reset(services);
		when(services.getNode("a")).thenThrow(NodeNotFoundException.class);

		MockHttpServletRequestBuilder request = get("/node/a");

		mockMvc.perform(request).andExpect(status().isNotFound());

		when(services.getNode("b")).thenThrow(IllegalArgumentException.class);

		request = get("/node/b");

		mockMvc.perform(request).andExpect(status().isBadRequest());

		
	
	}

	@Test
	public void testMoveNode() throws Exception {
		when(services.moveNodes(List.of("a"), "b", "username")).thenReturn(Node.builder().id(2).uniqueId("a").build());

		MockHttpServletRequestBuilder request = post("/move")
				.contentType(JSON)
				.content(objectMapper.writeValueAsString(List.of("a")))
				.param("to", "b")
				.param("username", "username");

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
	}

	@Test
	public void testMoveNodeUsernameEmpty() throws Exception {
		MockHttpServletRequestBuilder request = post("/move")
				.contentType(JSON)
				.content(objectMapper.writeValueAsString(List.of("a")))
				.param("to", "b")
				.param("username", "");

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void testMoveNodeTargetIdEmpty() throws Exception {
		MockHttpServletRequestBuilder request = post("/move")
				.contentType(JSON)
				.content(objectMapper.writeValueAsString(List.of("a")))
				.param("to", "")
				.param("username", "user");

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void testMoveNodeSourceNodeListEmpty() throws Exception {
		MockHttpServletRequestBuilder request = post("/move")
				.contentType(JSON)
				.content(objectMapper.writeValueAsString(Collections.emptyList()))
				.param("to", "targetId")
				.param("username", "user");

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void testMoveNodeNoUsername() throws Exception {
		MockHttpServletRequestBuilder request = post("/move").param("to", "b");

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void testUpdateConfig() throws Exception {

		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).userName("myusername").id(0).build();
		List<ConfigPv> configPvList = Collections.singletonList(ConfigPv.builder().id(1).pvName("name").build());
		
		UpdateConfigHolder holder = UpdateConfigHolder.builder().config(config).configPvList(configPvList).build();
		
		when(services.updateConfiguration(holder.getConfig(), holder.getConfigPvList())).thenReturn(config);

		MockHttpServletRequestBuilder request = post("/config/a/update").contentType(JSON)
				.content(objectMapper.writeValueAsString(holder));

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
	}
	
	@Test
	public void testUpdateConfigBadConfigPv() throws Exception {

		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).id(0).build();
		UpdateConfigHolder holder = UpdateConfigHolder.builder().build();
		
		MockHttpServletRequestBuilder request = post("/config/a/update").contentType(JSON)
				.content(objectMapper.writeValueAsString(holder));

		mockMvc.perform(request).andExpect(status().isBadRequest());
		
		holder.setConfig(config);	
		
		request = post("/config/a/update").contentType(JSON)
				.content(objectMapper.writeValueAsString(holder));
		
		mockMvc.perform(request).andExpect(status().isBadRequest());
		
		List<ConfigPv> configPvList = Collections.singletonList(ConfigPv.builder().build());
		holder.setConfigPvList(configPvList);
		
		when(services.updateConfiguration(holder.getConfig(), holder.getConfigPvList())).thenReturn(config);

		request = post("/config/a/update").contentType(JSON)
				.content(objectMapper.writeValueAsString(holder));

		mockMvc.perform(request).andExpect(status().isBadRequest());
		
		request = post("/config/a/update").contentType(JSON)
				.content(objectMapper.writeValueAsString(holder));

		mockMvc.perform(request).andExpect(status().isBadRequest());
		
		config.setUserName("");
		
		request = post("/config/a/update").contentType(JSON)
				.content(objectMapper.writeValueAsString(holder));

		mockMvc.perform(request).andExpect(status().isBadRequest());
		
		configPvList = Collections.singletonList(ConfigPv.builder().pvName("").build());
		holder.setConfigPvList(configPvList);
		
		request = post("/config/a/update").contentType(JSON)
				.content(objectMapper.writeValueAsString(holder));

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void testGetFolderIllegalArgument() throws Exception {
		when(services.getNode("a")).thenThrow(IllegalArgumentException.class);

		MockHttpServletRequestBuilder request = get("/node/a");

		mockMvc.perform(request).andExpect(status().isBadRequest());

	}

	@Test
	public void testUpdateNode() throws Exception {

		Node node = Node.builder().name("foo").uniqueId("a").build();

		when(services.updateNode(node, false)).thenReturn(node);

		MockHttpServletRequestBuilder request = post("/node/a/update")
				.param("customTimeForMigration", "false")
				.contentType(JSON)
				.content(objectMapper.writeValueAsString(node));

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);

	}
	
	@Test
	public void testGetConfigPvs() throws Exception{
		
		ConfigPv configPv = ConfigPv.builder()
				.id(1)
				.pvName("pvname")
				.build();
		
		when(services.getConfigPvs("cpv")).thenReturn(Collections.singletonList(configPv));
		
		MockHttpServletRequestBuilder request = get("/config/cpv/items");
			
		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();
		
		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<ConfigPv>>() {
		});
	}

	@Test
	public void testGetFromPath() throws Exception{
		when(services.getFromPath("/a/b/c")).thenReturn(null);
		MockHttpServletRequestBuilder request = get("/path?path=/a/b/c");
		mockMvc.perform(request).andExpect(status().isNotFound());

		request = get("/path");
		mockMvc.perform(request).andExpect(status().isBadRequest());

		Node node = Node.builder().name("name").uniqueId("uniqueId").build();
		when(services.getFromPath("/a/b/c")).thenReturn(Collections.singletonList(node));
		request = get("/path?path=/a/b/c");
		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		List<Node> nodes = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
		});

		assertEquals(1, nodes.size());
	}

	@Test
	public void testGetFullPath() throws Exception{
		when(services.getFullPath("nonexisting")).thenReturn(null);
		MockHttpServletRequestBuilder request = get("/path/nonexsiting");
		mockMvc.perform(request).andExpect(status().isNotFound());

		when(services.getFullPath("existing")).thenReturn("/a/b/c");
		request = get("/path/existing");
		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		assertEquals("/a/b/c", result.getResponse().getContentAsString());

	}
}
