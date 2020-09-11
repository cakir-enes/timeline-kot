package com.example.demo

data class FeedKafeDesc(val id: Int, val name: String, val code: String) {

    companion object {
        val info = listOf(
                FeedKafeDesc(0, "Yaay", "yaay"),
                FeedKafeDesc(1, "Borsa", "borsa"),
                FeedKafeDesc(2, "Kripto", "foreks"),
                FeedKafeDesc(3, "Haber", "haber"),
                FeedKafeDesc(4, "Sigorta", "sigorta"),
                FeedKafeDesc(5, "Tekno", "tekno"),
                FeedKafeDesc(6, "Emlak", "emlak"),
                FeedKafeDesc(7, "Oto", "oto"),
                FeedKafeDesc(8, "Genç", "genc"),
                FeedKafeDesc(9, "Magazin", "magazin"),
                FeedKafeDesc(10, "Eğitim", "egitim"),
                FeedKafeDesc(11, "Kariyer", "kariyer"),
                FeedKafeDesc(12, "Tatil", "tatil"),
                FeedKafeDesc(13, "Siyaset", "siyaset"),
                FeedKafeDesc(14, "Spor", "spor"),
                FeedKafeDesc(15, "Sağlık", "saglik"),
                FeedKafeDesc(16, "Moda", "moda"),
                FeedKafeDesc(17, "Tarih", "tarih"),
                FeedKafeDesc(18, "Gurme", "gurme"),
                FeedKafeDesc(19, "Astro", "astro"),
                FeedKafeDesc(20, "Kültür", "kultur"),
                FeedKafeDesc(21, "21", "21"),
                FeedKafeDesc(22, "22", "22"),
                FeedKafeDesc(23, "Mizah", "mizah"),
                FeedKafeDesc(24, "Çocuk", "cocuk"),
                FeedKafeDesc(25, "Dizi", "dizi"),
                FeedKafeDesc(26, "Müzik", "muzik")
        ).map { it.id to it }.toMap()

        fun forCategory(categoryId: Int): FeedKafeDesc? {
            val feedKafeDesc = info[categoryId]
            if (feedKafeDesc == null) {
                System.err.println("Unknown kafe id $categoryId")
            }
            return feedKafeDesc
        }

    }
}