package com.pfalabs.soak

import org.apache.jackrabbit.oak.api.ContentRepository
import org.apache.jackrabbit.oak.api.ContentSession
import javax.jcr.GuestCredentials
import org.apache.jackrabbit.oak.api.Root

object Sessions {

  def withLatestRoot[U](repository: ContentRepository, m: (Root) => U): U = {
    val s = guestSession(repository)
    val root = s.getLatestRoot();
    try {
      return m(root);
    } finally {
      s.close
    }
  }

  // ----------------------------------------------------
  // OAK SESSION
  // ----------------------------------------------------

  def guestSession(r: ContentRepository): ContentSession =
    r.login(new GuestCredentials(), null);

  def guestSession(repository: Option[ContentRepository]): Option[ContentSession] =
    repository match {
      case Some(r) ⇒ Some(guestSession(r));
      case None ⇒ None;
    }

  def isReadOnly(session: Option[ContentSession]): Boolean =
    session match {
      case Some(s) if (s.getAuthInfo().getUserID() != null && !"anonymous".equals(s.getAuthInfo().getUserID())) ⇒ return false;
      case _ ⇒ true;
    }

}