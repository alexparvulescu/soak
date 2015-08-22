package com.pfalabs.soak.osgi

import java.util.Hashtable
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.ReferencePolicy.STATIC
import org.apache.felix.scr.annotations.ReferencePolicyOption.GREEDY
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.ContentRepository
import org.apache.jackrabbit.oak.osgi.OsgiWhiteboard
import org.apache.jackrabbit.oak.plugins.commit.{ ConflictValidatorProvider, JcrConflictHandler }
import org.apache.jackrabbit.oak.plugins.itemsave.ItemSaveValidatorProvider
import org.apache.jackrabbit.oak.plugins.name.{ NameValidatorProvider, NamespaceEditorProvider }
import org.apache.jackrabbit.oak.plugins.nodetype.TypeEditorProvider
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent
import org.apache.jackrabbit.oak.plugins.version.VersionEditorProvider
import org.apache.jackrabbit.oak.spi.commit.EditorHook
import org.apache.jackrabbit.oak.spi.security.SecurityProvider
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.apache.jackrabbit.oak.spi.whiteboard.{ Whiteboard, WhiteboardIndexEditorProvider, WhiteboardIndexProvider }
import org.osgi.framework.ServiceRegistration
import org.osgi.service.component.ComponentContext
import java.io.Closeable
import org.apache.commons.io.IOUtils

trait OakService {

  val DEFAULT_WORKSPACE_NAME = "oak";

  @Reference(policy = STATIC, policyOption = GREEDY)
  var store: NodeStore = null

  @Reference(policy = STATIC, policyOption = GREEDY)
  val securityProvider: SecurityProvider = null

  val indexProvider = new WhiteboardIndexProvider()

  val indexEditorProvider = new WhiteboardIndexEditorProvider()

  var repositoryServiceReference: Option[(ServiceRegistration, OSGiContentRepository)] = None

  def doActivate(context: ComponentContext) {
    val whiteboard = new OsgiWhiteboard(context.getBundleContext())

    indexProvider.start(whiteboard)
    indexEditorProvider.start(whiteboard)
    val repository = new OSGiContentRepository(this.createRepository(whiteboard))
    val registration = context.getBundleContext().registerService(classOf[ContentRepository].getName(), repository, new Hashtable[String, Object]())
    repositoryServiceReference = Some(registration, repository)
  }

  def doDeactivate() {
    indexProvider.stop();
    indexEditorProvider.stop();

    repositoryServiceReference.foreach(r => {
      r._1.unregister()
      r._2.close()
    })
    repositoryServiceReference = None;
  }

  //----------------------------------------------------------------------------------------------------< private >---

  def createRepository(whiteboard: Whiteboard) = {

    val oak = new Oak(store)
      .`with`(whiteboard)
      .`with`(new InitialContent().withPrePopulatedVersionStore())

      .`with`(JcrConflictHandler.createJcrConflictHandler)
      .`with`(new EditorHook(new VersionEditorProvider()))

      .`with`(securityProvider)

      .`with`(new ItemSaveValidatorProvider())
      .`with`(new NameValidatorProvider())
      .`with`(new NamespaceEditorProvider())
      .`with`(new TypeEditorProvider())
      .`with`(new ConflictValidatorProvider())

      // index stuff
      .`with`(indexProvider)
      .`with`(indexEditorProvider)

      // ws stuff
      .`with`(DEFAULT_WORKSPACE_NAME)

    oak.createContentRepository()
  }

}