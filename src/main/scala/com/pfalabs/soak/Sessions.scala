package com.pfalabs.soak

import org.apache.jackrabbit.oak.api.ContentRepository
import org.apache.jackrabbit.oak.api.ContentSession
import javax.jcr.GuestCredentials
import org.apache.jackrabbit.oak.api.Root
import javax.jcr.SimpleCredentials
import javax.jcr.Credentials
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import java.io.Closeable
import org.apache.commons.io.IOUtils

object Sessions {

  // ----------------------------------------------------
  // OAK SESSION OPS
  // ----------------------------------------------------

  type RepoOpF[U] = (Root) => U

  def run[U](u: String, p: String)(o: RepoOpF[U])(implicit r: ContentRepository): Try[U] =
    login(u, p)(r).map { s => run(s)(o) }

  def run[U](c: Credentials)(o: RepoOpF[U])(implicit r: ContentRepository): Try[U] =
    login(c)(r).map { s => run(s)(o) }

  def runAsAdmin[U](o: RepoOpF[U])(implicit r: ContentRepository): Try[U] =
    asAdmin(r).map { s => try { run(s)(o) } finally { s.close } }

  def runAsGuest[U](o: RepoOpF[U])(implicit r: ContentRepository): Try[U] =
    asGuest(r).map { s => try { run(s)(o) } finally { s.close } }

  def run[U](s: ContentSession)(o: RepoOpF[U]): U =
    o.apply(s.getLatestRoot)

  // ----------------------------------------------------
  // OAK SESSION LOGIN
  // ----------------------------------------------------

  def asAdmin(implicit r: ContentRepository): Try[ContentSession] = login("admin", "admin")(r)

  def login(user: String, password: String)(implicit r: ContentRepository): Try[ContentSession] =
    login(new SimpleCredentials(user, password.toCharArray()))(r)

  def asGuest(implicit r: ContentRepository): Try[ContentSession] = login(new GuestCredentials())(r)

  def login(c: Credentials)(implicit r: ContentRepository): Try[ContentSession] = {
    try {
      Success(r.login(c, null))
    } catch {
      case e: Throwable => Failure(e)
    }
  }

  // ----------------------------------------------------
  // OAK REPOSITORY
  // ----------------------------------------------------

  def close(r: ContentRepository) = {
    if (r.isInstanceOf[Closeable]) {
      IOUtils.closeQuietly(r.asInstanceOf[Closeable])
    }
  }
}
