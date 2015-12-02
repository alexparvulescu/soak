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

trait OakService {

  var nodeStore: NodeStore = null

  var securityProvider: SecurityProvider = null

  // TODO enable this as soon as Oak 1.3.6 is out
  // /**
  //   * Reference needed because of OAK-3366
  //   */
  //  @Reference(referenceInterface = classOf[IndexEditorProvider],
  //    target = "(type=property)",
  //    strategy = ReferenceStrategy.LOOKUP)
  //  val pi: IndexEditorProvider = null

  val editorProvider = new WhiteboardEditorProvider()

  val indexProvider = new WhiteboardIndexProvider()

  val indexEditorProvider = new WhiteboardIndexEditorProvider()

  var initializers: Option[Tracker[RepositoryInitializer]] = None

  var repositoryServiceReference: Option[(ServiceRegistration[_], OSGiContentRepository)] = None

  def doActivate(context: ComponentContext) {
    val whiteboard = new OsgiWhiteboard(context.getBundleContext())
    initializers = Some(whiteboard.track(classOf[RepositoryInitializer]))
    editorProvider.start(whiteboard)
    indexProvider.start(whiteboard)
    indexEditorProvider.start(whiteboard)
    val repository = new OSGiContentRepository(this.createRepository(whiteboard))
    val registration = context.getBundleContext().registerService(classOf[ContentRepository].getName(), repository, new Hashtable[String, Object]())
    repositoryServiceReference = Some((registration, repository))
  }

  def doDeactivate() {
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

  def createRepository(whiteboard: Whiteboard) = {

    val oak = new Oak(nodeStore)
      .`with`(whiteboard)
      .`with`(new InitialContent())
      .`with`(JcrConflictHandler.createJcrConflictHandler)

      .`with`(securityProvider)
      .`with`(editorProvider)
      .`with`(indexProvider)
      .`with`(indexEditorProvider)

    initializers.map { _.getServices.foreach(oak.`with`(_)) }

    oak.createContentRepository()
  }

  @Reference(name = "nodeStore")
  def setNodeStore(s: NodeStore) {
    nodeStore = s
  }

  def unsetNodeStore(s: NodeStore) {
    nodeStore = null
  }

  @Reference(name = "securityProvider")
  def setSecurityProvider(s: SecurityProvider) {
    securityProvider = s
  }

  def unsetSecurityProvider(s: SecurityProvider) {
    securityProvider = null
  }

}