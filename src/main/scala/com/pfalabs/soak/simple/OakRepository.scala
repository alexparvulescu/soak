package com.pfalabs.soak.simple

import java.io.File

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.immutable.Map

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.ContentRepository
import org.apache.jackrabbit.oak.plugins.commit.{ ConflictValidatorProvider, JcrConflictHandler }
import org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndexProvider
import org.apache.jackrabbit.oak.plugins.index.property.{ PropertyIndexEditorProvider, PropertyIndexProvider }
import org.apache.jackrabbit.oak.plugins.index.reference.{ ReferenceEditorProvider, ReferenceIndexProvider }
import org.apache.jackrabbit.oak.plugins.name.{ NameValidatorProvider, NamespaceEditorProvider }
import org.apache.jackrabbit.oak.plugins.nodetype.TypeEditorProvider
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore
import org.apache.jackrabbit.oak.plugins.version.VersionEditorProvider
import org.apache.jackrabbit.oak.security.SecurityProviderImpl
import org.apache.jackrabbit.oak.spi.commit.EditorHook
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters
import org.apache.jackrabbit.oak.spi.security.authentication.ConfigurationUtil
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants
import org.apache.jackrabbit.oak.spi.security.user.{ UserConfiguration, UserConstants }
import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.apache.jackrabbit.oak.spi.xml.{ ImportBehavior, ProtectedItemImporter }

import javax.security.auth.login.Configuration

@deprecated("not maintained")
trait OakRepository {

  var repository: Option[ContentRepository] = None;

  var store: Option[NodeStore] = None

  def initOak(fname: String) = {
    Configuration.setConfiguration(ConfigurationUtil.getJackrabbit2Configuration(ConfigurationParameters.EMPTY));
    store = Some(newNodeStore(fname))
    repository = Some(createRepository(store.get))
  }

  // ----------------------------------------------------
  // OAK REPOSITORY
  // ----------------------------------------------------

  private def newNodeStore(fname: String) = new SegmentNodeStore(new FileStore(new File(fname), 256, true))

  private def createRepository(store: NodeStore): ContentRepository = (new Oak(store))
    .`with`(new InitialContent())

    .`with`(JcrConflictHandler.createJcrConflictHandler)
    .`with`(new EditorHook(new VersionEditorProvider()))

    .`with`(new SecurityProviderImpl(buildSecurityConfig()))

    .`with`(new NameValidatorProvider())
    .`with`(new NamespaceEditorProvider())
    .`with`(new TypeEditorProvider())
    .`with`(new ConflictValidatorProvider())
    .`with`(new ReferenceEditorProvider())
    .`with`(new ReferenceIndexProvider())

    .`with`(new PropertyIndexEditorProvider())

    .`with`(new PropertyIndexProvider())
    .`with`(new NodeTypeIndexProvider())

    //      .`with`(new LuceneInitializerHelper("luceneGlobal", LuceneIndexHelper.JR_PROPERTY_INCLUDES).async())
    //      .`with`(new LuceneIndexEditorProvider())
    //      .`with`(new LuceneIndexProvider())
    //      .withAsyncIndexing()
    .createContentRepository();

  // ----------------------------------------------------
  // OAK Security Setup?
  // ----------------------------------------------------

  private def buildSecurityConfig(): ConfigurationParameters = {
    val userConfig: Map[String, Object] = Map(
      UserConstants.PARAM_GROUP_PATH -> "/home/groups",
      UserConstants.PARAM_USER_PATH -> "/home/users",
      UserConstants.PARAM_DEFAULT_DEPTH -> new Integer(1),
      AccessControlAction.USER_PRIVILEGE_NAMES -> Array(PrivilegeConstants.JCR_ALL),
      AccessControlAction.GROUP_PRIVILEGE_NAMES -> Array(PrivilegeConstants.JCR_READ),
      ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR -> ImportBehavior.NAME_BESTEFFORT);
    return ConfigurationParameters.of(Map(
      UserConfiguration.NAME -> ConfigurationParameters.of(userConfig)));
  }
}