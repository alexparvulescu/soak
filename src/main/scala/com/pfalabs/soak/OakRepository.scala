package com.pfalabs.soak

import java.io.File

import scala.collection.JavaConversions.mapAsJavaMap

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.ContentRepository
import org.apache.jackrabbit.oak.api.ContentSession
import org.apache.jackrabbit.oak.plugins.commit.ConflictValidatorProvider
import org.apache.jackrabbit.oak.plugins.commit.JcrConflictHandler
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexEditorProvider
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProvider
import org.apache.jackrabbit.oak.plugins.index.lucene.util.LuceneIndexHelper
import org.apache.jackrabbit.oak.plugins.index.lucene.util.LuceneInitializerHelper
import org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndexProvider
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexEditorProvider
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexProvider
import org.apache.jackrabbit.oak.plugins.name.NameValidatorProvider
import org.apache.jackrabbit.oak.plugins.name.NamespaceValidatorProvider
import org.apache.jackrabbit.oak.plugins.nodetype.RegistrationEditorProvider
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
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration
import org.apache.jackrabbit.oak.spi.security.user.UserConstants
import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction
import org.apache.jackrabbit.oak.spi.xml.ImportBehavior
import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter

import javax.jcr.GuestCredentials
import javax.jcr.SimpleCredentials
import javax.security.auth.login.Configuration

trait OakRepository {

  var repository: Option[ContentRepository] = None;

  def initOak(fname: String) = {
    Configuration.setConfiguration(ConfigurationUtil.getJackrabbit2Configuration(ConfigurationParameters.EMPTY));
    repository = Some(createRepository(fname))
  }

  def login(u: String, p: String): Either[String, ContentSession] = login(repository.get, u, p) match {
    case Right(u) ⇒ {
      return Right(u);
    }
    case Left(u) ⇒ {
      return Left(u);
    }
  }

  // ----------------------------------------------------
  // OAK REPOSITORY
  // ----------------------------------------------------

  private[soak] def createRepository(fname: String): ContentRepository =
    new Oak(new SegmentNodeStore(new FileStore(new File(fname), 268435456, true)))
      .`with`(new InitialContent())

      .`with`(JcrConflictHandler.JCR_CONFLICT_HANDLER)
      .`with`(new EditorHook(new VersionEditorProvider()))

      .`with`(new SecurityProviderImpl(buildSecurityConfig()))

      .`with`(new NameValidatorProvider())
      .`with`(new NamespaceValidatorProvider())
      .`with`(new TypeEditorProvider())
      .`with`(new RegistrationEditorProvider())
      .`with`(new ConflictValidatorProvider())

      .`with`(new PropertyIndexEditorProvider())

      .`with`(new PropertyIndexProvider())
      .`with`(new NodeTypeIndexProvider())

      .`with`(new LuceneInitializerHelper("luceneGlobal", LuceneIndexHelper.JR_PROPERTY_INCLUDES).async())
      .`with`(new LuceneIndexEditorProvider())
      .`with`(new LuceneIndexProvider())
      .createContentRepository();

  // ----------------------------------------------------
  // OAK SESSION
  // ----------------------------------------------------

  private def guestSession(repository: Option[ContentRepository]): Option[ContentSession] =
    repository match {
      case Some(r) ⇒ Some(r.login(new GuestCredentials(), null));
      case None ⇒ None;
    }

  private def login(repo: ContentRepository, u: String, p: String): Either[String, ContentSession] =
    try {
      return Right(repo.login(new SimpleCredentials(u, p.toCharArray()), null));
    } catch {
      case e: Exception ⇒ {
        e.printStackTrace()
        Left(e.getMessage());
      }
    }

  // ----------------------------------------------------
  // OAK Security Setup?
  // ----------------------------------------------------

  import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
  import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
  import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
  import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction;
  import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
  import org.apache.jackrabbit.oak.spi.xml.ImportBehavior;
  import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter;
  import scala.collection.JavaConversions._

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