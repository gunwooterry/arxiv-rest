package models

import org.joda.time.DateTime

case class Paper(id: String,
                 title: String,
                 authors: Seq[String],
                 subjects: Seq[String],
                 published: DateTime,
                 updated: DateTime,
                 summary: String,
                 comment: Option[String],
                 journalRef: Option[String])
