package org.alexeyn.news

final case class Data(news: List[Headline])
final case class NewsResponse(data: Data)
