package com.pfalabs.soak.osgi

import java.util.Hashtable

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.ContentRepository
import org.apache.jackrabbit.oak.osgi.OsgiWhiteboard
import org.apache.jackrabbit.oak.plugins.commit.JcrConflictHandler
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer
import org.apache.jackrabbit.oak.spi.security.SecurityProvider
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.apache.jackrabbit.oak.spi.whiteboard.{ Tracker, Whiteboard, WhiteboardEditorProvider, WhiteboardIndexEditorProvider, WhiteboardIndexProvider }
import org.osgi.framework.ServiceRegistration
import org.osgi.service.component.ComponentContext
import org.osgi.service.component.annotations.Reference
import org.apache.jackrabbit.oak.plugins.index.IndexEditorProvider

import org.osgi.service.component.annotations.{ Activate, Component, Deactivate }
import org.osgi.service.component.annotations.ConfigurationPolicy

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
class OakService {

  var nodeStore: NodeStore = null

  var securityProvider: SecurityProvider = null

  var propertyIndex: IndexEditorProvider = null

  var referenceIndex: IndexEditorProvider = null

  val editorProvider = new WhiteboardEditorProvider()

  val indexProvider = new WhiteboardIndexProvider()

  val indexEditorProvider = new WhiteboardIndexEditorProvider()

  var initializers: Option[Tracker[RepositoryInitializer]] = None

  var repositoryServiceReference: Option[(ServiceRegistration[_], OSGiContentRepository)] = None

  @Activate
  def activate(context: ComponentContext) {
    val whiteboard = new OsgiWhiteboard(context.getBundleContext())
    initializers = Some(whiteboard.track(classOf[RepositoryInitializer]))
    editorProvider.start(whiteboard)
    indexProvider.start(whiteboard)
    indexEditorProvider.start(whiteboard)
    val repository = new OSGiContentRepository(this.createRepository(whiteboard))
    val registration = context.getBundleContext().registerService(classOf[ContentRepository].getName(), repository, new Hashtable[String, Object]())
    repositoryServiceReference = Some((registration, repository))
  }

  @Deactivate
  def deactivate() {
    initializers.foreach(ri ⇒ { ri.stop })
    initializers = None

    editorProvider.stop()
    indexProvider.stop()
    indexEditorProvider.stop()

    repositoryServiceReference.foreach(r ⇒ {
      r._1.unregister()
      r._2.close()
    })
    repositoryServiceReference = None
  }

  //----------------------------------------------------------------------------------------------------< private >---

  def createRepository(whiteboard: Whiteboard): ContentRepository = {

    val oak = new Oak(nodeStore)
      .`with`(new InitialContent())
      .`with`(JcrConflictHandler.createJcrConflictHandler)
      .`with`(whiteboard)
      .`with`(securityProvider)
      .`with`(editorProvider)
      .`with`(indexEditorProvider)
      .`with`(indexProvider)
      .withFailOnMissingIndexProvider()
    //TODO no async indexing yet
    //   .withAsyncIndexing()

    initializers.map { _.getServices.foreach(oak.`with`(_)) }

    oak.createContentRepository()
  }

  @Reference(name = "nodeStore")
  def setNodeStore(ns: NodeStore) {
    nodeStore = ns
  }

  def unsetNodeStore(ns: NodeStore) {
    nodeStore = null
  }

  @Reference(name = "securityProvider")
  def setSecurityProvider(s: SecurityProvider) {
    securityProvider = s
  }

  def unsetSecurityProvider(s: SecurityProvider) {
    securityProvider = null
  }

  @Reference(name = "propertyIndex", target = "(type=property)")
  def setPropertyIndex(i: IndexEditorProvider) {
    propertyIndex = i
  }

  def unsetPropertyIndex(i: NodeStore) {
    propertyIndex = null
  }

  @Reference(name = "referenceIndex", target = "(type=reference)")
  def setReferenceIndex(i: IndexEditorProvider) {
    referenceIndex = i
  }

  def unsetReferenceIndex(i: NodeStore) {
    referenceIndex = null
  }
}