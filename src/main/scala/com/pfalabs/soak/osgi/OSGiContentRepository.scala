package com.pfalabs.soak.osgi

import org.apache.jackrabbit.oak.api.ContentRepository
import javax.jcr.Credentials
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.ContentSession

class OSGiContentRepository(r: ContentRepository) extends ContentRepository {

  override def login(credentials: Credentials, workspace: String): ContentSession = {
    val thread = Thread.currentThread()
    val loader = thread.getContextClassLoader
    try {
      thread.setContextClassLoader(classOf[Oak].getClassLoader)
      r.login(credentials, workspace)
    } finally {
      thread.setContextClassLoader(loader)
    }
  }

  override def getDescriptors() = r.getDescriptors()

}