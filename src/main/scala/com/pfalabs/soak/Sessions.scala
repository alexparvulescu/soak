package com.pfalabs.soak

import org.apache.jackrabbit.oak.api.ContentSession
import org.apache.jackrabbit.oak.api.ContentRepository
import javax.jcr.SimpleCredentials
import javax.jcr.GuestCredentials

object Sessions {

  // ----------------------------------------------------
  // OAK SESSION
  // ----------------------------------------------------

  def guestSession(repository: Option[ContentRepository]): Option[ContentSession] =
    repository match {
      case Some(r) ⇒ Some(r.login(new GuestCredentials(), null));
      case None ⇒ None;
    }

  def isReadOnly(session: Option[ContentSession]): Boolean =
    session match {
      case Some(s) if (s.getAuthInfo().getUserID() != null && !"anonymous".equals(s.getAuthInfo().getUserID())) ⇒ return false;
      case _ ⇒ true;
    }

}