# -*- coding: utf-8 -*-
"""Word lists of tour 6, lot 5 (one per change). (indefinite, definite) for Arabic multi-word entries."""

AR_PROTECTED = ["Drum & bass", "Pad Live", "Pad المباشر", "→ Pad", "drum loop", "warm pad", "Think break",
                "dark techno kick", "«kick»", "«echo»", "«reverb»", "Master KG", "Bass music"]

# n° 6: the names of the song's parts (Scenes.SECTIONS, DRUM_LABELS) and the texts naming them.
AR_SECTIONS = {
    "Intro": ("مقدمة", "المقدمة"),
    "Outro": ("خاتمة", "الخاتمة"),
    "Drop": ("دروب", "الدروب"),
    "Drops": ("دروبات", "الدروبات"),
    "Break": ("فاصل", "الفاصل"),
    "Build-up": ("تصاعد", "التصاعد"),
    "Build-ups": ("تصاعدات", "التصاعدات"),
    "Bridge": ("جسر", "الجسر"),
    "Pre-chorus": ("ما قبل اللازمة", "ما قبل اللازمة"),
    "Post-chorus": ("ما بعد اللازمة", "ما بعد اللازمة"),
    "Groove": ("غروف", "الغروف"),
    "Fake drop": ("دروب زائف", "الدروب الزائف"),
    "Mini-break": ("فاصل قصير", "الفاصل القصير"),
    "Fill": ("حشوة", "الحشوة"),
    "Fills": ("حشوات", "الحشوات"),
}

HI_SECTIONS = {
    "Intro": "इंट्रो", "Outro": "आउट्रो", "Drop": "ड्रॉप", "Drops": "ड्रॉप", "drop": "ड्रॉप", "Break": "ब्रेक",
    "Build-up": "बिल्ड-अप", "Bridge": "ब्रिज", "Pre-chorus": "प्री-कोरस", "Post-chorus": "पोस्ट-कोरस",
    "Groove": "ग्रूव", "groove": "ग्रूव", "Fake drop": "फ़ेक ड्रॉप", "Mini-break": "मिनी-ब्रेक",
    "Fill": "फ़िल", "Fills": "फ़िल", "funky groove": "फ़ंकी ग्रूव",
}

ZH_SECTIONS = {
    "Intro": "前奏", "Outro": "尾奏", "Drop": "高潮", "Drops": "高潮", "Break": "间奏", "Build-up": "渐强段",
    "Bridge": "桥段", "Groove": "律动", "Mini-break": "短间奏", "Fill": "加花", "Fills": "加花", "Crash": "吊镲",
}

# Agreement and sense, by French key (the value for that stage).
AR_SECTIONS_OVERRIDES = {
    "Appui long sur une partie : ajoute un Fill, un Break ou une Montée.": "ضغطة مطوّلة على جزء: أضف حشوة أو فاصلًا أو تصاعدًا.",
    "Taper mon propre fill": "انقر الحشوة الخاصة بي",
    "La batterie : chaque partie finit ses 4 mesures par un petit roulement, qui annonce la suite.": "الطبول: كل جزء ينهي مازوراته الأربع بحشوة صغيرة تعلن ما سيأتي.",
    # « break » here is the drum break of an old record (breakbeat), not the part
    "Petites percussions discrètes qui imitent un « break » : un bout de batterie de vieux disque funk, découpé et accéléré.": "إيقاعيات صغيرة هادئة تقلّد «بريك»: مقطع طبول من أسطوانة Funk قديمة، مقطّع ومسرَّع.",
    "Un shaker léger, en plus du charley : il imite le « break » de vieux disque soul qu'on glisse sous la batterie en liquid.": "Shaker خفيف فوق الـHi-hat: يقلّد «بريك» أسطوانات الـSoul القديمة المنسلّ تحت الطبول في الـLiquid.",
    "Un petit roulement de toms toutes les deux mesures, pour relancer comme un batteur des années 80.": "حشوة ‏Toms صغيرة كل مازورتين، للدفع كعازف طبول من الثمانينيات.",
}

DICTS = {
    "ar-sections": {"lang": "ar", "words": AR_SECTIONS, "case": False, "protected": AR_PROTECTED, "overrides": AR_SECTIONS_OVERRIDES},
    "hi-sections": {"lang": "hi", "words": HI_SECTIONS, "case": True, "protected": ["Think break", "Drum & bass"]},
    "zh-sections": {"lang": "zh-CN", "words": ZH_SECTIONS, "case": True, "protected": ["Think break", "Drum & bass"]},
}

# Masculine words where the Arabic texts agree with a masculine Latin word (« Roll صغير », « Stab معدني »).
# R15: the other trade's words of the Arabic catalogs, the Mode simple's words (mots_simples.json) unless said.
AR_TRADE = dict(AR_SECTIONS)
AR_TRADE.update({
    "Kick": ("طبل كبير", "الطبل الكبير"),
    "Kicks": ("طبول كبيرة", "الطبول الكبيرة"),
    "Snare": ("سنير", "السنير"), "Snares": ("سنير", "السنير"),
    "Loop": ("تكرار", "التكرار"), "Loops": ("تكرارات", "التكرارات"),
    "Swing": ("تأرجح", "التأرجح"),
    "Pad": ("باد", "الباد"), "Pads": ("بادات", "البادات"),
    # « Solo » the button: playing alone
    "Solo": ("عزف منفرد", "العزف المنفرد"),
    "Crash": ("صنج", "الصنج"),
    "Hi-hat": ("هاي هات", "الهاي هات"), "Hi-hats": ("هاي هات", "الهاي هات"),
    "Sub bass": ("باص عميق", "الباص العميق"),
    "Clap": ("تصفيق", "التصفيق"), "Claps": ("تصفيق", "التصفيق"),
    "Ride": ("صنج الإيقاع", "صنج الإيقاع"),
    "Hook": ("هوك", "الهوك"),
    "Bass music": ("موسيقى الباص", "موسيقى الباص"),
    "Mix": ("مزج", "المزج"),
    "Tom": ("توم", "التوم"), "Toms": ("توم", "التوم"),
    "Maracas": ("ماراكاس", "الماراكاس"),
    "Conga": ("كونغا", "الكونغا"), "Congas": ("كونغا", "الكونغا"),
    "Cymbal": ("صنج", "الصنج"),
    "Bass": ("باص", "الباص"), "Basses": ("باصات", "الباصات"),
    "Synth": ("سينث", "السينث"), "Synths": ("سينث", "السينث"),
    "Echo": ("صدى", "الصدى"),
    "Reverb": ("صدى المكان", "صدى المكان"),
    "Shaker": ("شيكر", "الشيكر"),
    "Cowbell": ("كاوبل", "الكاوبل"), "Rim": ("ريم", "الريم"), "Zap": ("زاب", "الزاب"),
    # Tour 6, lot 5: the Live's effects, said in Arabic as Japanese says them in katakana.
    "Repeat": ("ترديد", "الترديد"), "Stutter": ("تقطيع", "التقطيع"), "Chopper": ("مقطِّع", "المقطِّع"),
    "Brake": ("فرملة", "الفرملة"), "Backspin": ("لفّ عكسي", "اللفّ العكسي"), "Tape stop": ("إيقاف الشريط", "إيقاف الشريط"),
    "Reverse": ("معكوس", "المعكوس"), "Half time": ("نصف السرعة", "نصف السرعة"),
    "High-pass": ("تمرير عالٍ", "التمرير العالي"), "Low-pass": ("تمرير منخفض", "التمرير المنخفض"),
    "Wobble": ("ووبل", "الووبل"), "Fade-in": ("ظهور تدريجي", "الظهور التدريجي"), "Fade-out": ("تلاشٍ تدريجي", "التلاشي التدريجي"),
    "Flanger": ("فلانجر", "الفلانجر"), "Phaser": ("فيزر", "الفيزر"),
    "Metronome": ("مترونوم", "المترونوم"), "Fader": ("فيدر", "الفيدر"), "Faders": ("فيدرات", "الفيدرات"),
    "Sub": ("باص عميق", "الباص العميق"),
    "Master": ("الصوت النهائي", "الصوت النهائي"),
    "Pan": ("موضع", "الموضع"),
    "Roll": ("رول", "الرول"), "Rolls": ("رولات", "الرولات"),
    "Drum": ("طبل", "الطبل"), "Drums": ("طبول", "الطبول"),
    "Stab": ("ستاب", "الستاب"), "Stabs": ("ستابات", "الستابات"),
    "Sample": ("عيّنة", "العيّنة"), "Samples": ("عيّنات", "العيّنات"),
    "Filter": ("فلتر", "الفلتر"),
    "Lead": ("اللحن", "اللحن"),
    "Pluck": ("بلك", "البلك"),
    # « النمط » is the style: a pattern is a rhythmic one
    "Pattern": ("نمط إيقاعي", "النمط الإيقاعي"),
    "Riff": ("ريف", "الريف"),
    "Noise": ("ضجيج", "الضجيج"),
    "Open": ("مفتوح", "المفتوح"),
    "Low": ("منخفض", "المنخفض"),
    "Resonance": ("رنين", "الرنين"),
    "Ghost bass": ("باص شبحي", "الباص الشبحي"),
})

AR_TRADE_OVERRIDES = {
    "Place (panoramique)": "الموضع (يمين-يسار)",
    "Place (pan)": "الموضع (يمين-يسار)",
    "Place · %s": "الموضع · %s",
    "Où un son se trouve entre la gauche et la droite (on dit aussi « pan »). D'abord tout au centre, puis le clap à gauche et la mélodie à droite.": "مكان الصوت بين اليسار واليمين (ويُسمّى أيضًا «البان»). أولًا كل شيء في الوسط، ثم التصفيق على اليسار واللحن على اليمين.",
    "Un petit roulement de timbales (des toms aigus) en fin de boucle, pour relancer.": "رول ‏Timbale صغير (طبول توم عالية) في نهاية التكرار، لإعادة الإطلاق.",
    "« Générer » fabrique une boucle à partir d'une phrase (Sons › Voix › Générer), puis la pose sur un pad live libre.": "«توليد» يصنع تكرارًا من جملة (الأصوات › الغناء › توليد)، ثم يضعه على باد حي حر.",
    "Génère-moi un pattern": "اصنع لي نمطًا إيقاعيًا",
    "Le son que joue le pad quand on le touche. Un « sample » (échantillon en français), c'est un son enregistré qu'on rejoue : un kick, une voix, un bout de morceau. Prends-le dans la bibliothèque, importe-le ou enregistre-le au micro ; sans sample, le pad joue le son de la boîte à rythmes.": "الصوت الذي يعزفه الباد عندما تضغطه. «العيّنة» صوت مسجَّل يُعاد تشغيله: طبل كبير، صوت بشري، مقطع من أغنية. خذ واحدًا من المكتبة أو استورده أو سجّله بالميكروفون؛ بدون عيّنة يعزف الباد صوت آلة الطبول.",
}
DICTS["ar-trade"] = {"lang": "ar", "words": AR_TRADE, "case": False, "protected": [p for p in AR_PROTECTED if p != "Bass music"], "overrides": AR_TRADE_OVERRIDES}
