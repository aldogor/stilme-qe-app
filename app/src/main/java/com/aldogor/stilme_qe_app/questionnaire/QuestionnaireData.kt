package com.aldogor.stilme_qe_app.questionnaire

import com.aldogor.stilme_qe_app.study.*

/**
 * Complete questionnaire definitions for STILME-QE study.
 * All questions in Italian as per study requirements.
 */
object QuestionnaireData {

    // ========================================================================
    // RESPONSE OPTIONS
    // ========================================================================

    val YESNO_OPTIONS = listOf(
        QuestionOption(0, "No"),
        QuestionOption(1, "Sì")
    )

    val AGE_GROUP_OPTIONS = listOf(
        QuestionOption(1, "18-21 anni"),
        QuestionOption(2, "22-26 anni"),
        QuestionOption(3, "27-30 anni"),
        QuestionOption(99, "Altro")
    )

    val GENDER_OPTIONS = listOf(
        QuestionOption(1, "Uomo cisgender (sesso maschile alla nascita, identità di genere maschile)"),
        QuestionOption(2, "Uomo transgender (sesso femminile alla nascita, identità di genere maschile)"),
        QuestionOption(3, "Donna cisgender (sesso femminile alla nascita, identità di genere femminile)"),
        QuestionOption(4, "Donna transgender (sesso maschile alla nascita, identità di genere femminile)"),
        QuestionOption(5, "Non-binario (identità di genere al di fuori del binario uomo/donna)"),
        QuestionOption(6, "Altro"),
        QuestionOption(7, "Preferisco non rispondere")
    )

    val NATIONALITY_OPTIONS = listOf(
        QuestionOption(1, "Italiana"),
        QuestionOption(2, "Non italiana")
    )

    val FIELD_OF_STUDY_OPTIONS = listOf(
        QuestionOption(1, "Area Economica"),
        QuestionOption(2, "Area Giuridica e Politico-Sociale"),
        QuestionOption(3, "Area Sanitaria"),
        QuestionOption(4, "Area Scientifica"),
        QuestionOption(5, "Area Umanistica")
    )

    val OUT_OF_TOWN_OPTIONS = listOf(
        QuestionOption(0, "No"),
        QuestionOption(1, "Sì, provengo dalla stessa regione della mia Università"),
        QuestionOption(2, "Sì, provengo da una regione diversa da quella della mia Università")
    )

    val LIVING_ARRANGEMENT_OPTIONS = listOf(
        QuestionOption(1, "Da solo/a"),
        QuestionOption(2, "Con i genitori"),
        QuestionOption(3, "Con parenti"),
        QuestionOption(4, "Con il/la partner"),
        QuestionOption(5, "Con coinquilini/e"),
        QuestionOption(6, "In dormitorio/residenza/collegio")
    )

    val FAMILY_COHESION_OPTIONS = listOf(
        QuestionOption(1, "Molto scarso"),
        QuestionOption(2, "Scarso"),
        QuestionOption(3, "Buono"),
        QuestionOption(4, "Ottimo"),
        QuestionOption(5, "Eccessivo")
    )

    val RELATIONSHIP_OPTIONS = listOf(
        QuestionOption(1, "Single"),
        QuestionOption(2, "Impegnato/a")
    )

    val SEXUAL_ORIENTATION_OPTIONS = listOf(
        QuestionOption(1, "Omosessuale"),
        QuestionOption(2, "Eterosessuale"),
        QuestionOption(3, "Bisessuale"),
        QuestionOption(4, "Asessuale"),
        QuestionOption(5, "Utilizzo un termine diverso"),
        QuestionOption(6, "Non lo so"),
        QuestionOption(7, "Preferisco non rispondere")
    )

    val ECONOMIC_SITUATION_OPTIONS = listOf(
        QuestionOption(1, "Insufficiente"),
        QuestionOption(2, "Scarsa"),
        QuestionOption(3, "Adeguata"),
        QuestionOption(4, "Ottima")
    )

    val EMPLOYMENT_OPTIONS = listOf(
        QuestionOption(0, "No"),
        QuestionOption(1, "Sì, per necessità e mi mantengo quasi/del tutto autonomamente"),
        QuestionOption(2, "Sì, per necessità ma non riesco a mantenermi"),
        QuestionOption(3, "Sì, ma non per necessità")
    )

    val SPORT_FREQUENCY_OPTIONS = listOf(
        QuestionOption(0, "No"),
        QuestionOption(1, "Sì, saltuariamente (meno di 1 volta a settimana)"),
        QuestionOption(2, "Sì, meno di 90 minuti a settimana"),
        QuestionOption(3, "Sì, più di 90 minuti a settimana")
    )

    val SUBSTANCE_USE_OPTIONS = listOf(
        QuestionOption(1, "Mai usata"),
        QuestionOption(2, "1 volta"),
        QuestionOption(3, "2-5 volte"),
        QuestionOption(4, "6-10 volte"),
        QuestionOption(5, "Più di 10 volte")
    )

    val CIGARETTES_OPTIONS = listOf(
        QuestionOption(0, "Non fumo"),
        QuestionOption(1, "0-5 sigarette a settimana"),
        QuestionOption(2, "1-5 sigarette al giorno"),
        QuestionOption(3, "6-10 sigarette al giorno"),
        QuestionOption(4, "11-15 sigarette al giorno"),
        QuestionOption(5, "16-20 sigarette al giorno"),
        QuestionOption(6, "Più di 20 sigarette al giorno")
    )

    val ALCOHOL_FREQUENCY_OPTIONS = listOf(
        QuestionOption(1, "Mai"),
        QuestionOption(2, "Mensilmente o meno"),
        QuestionOption(3, "2-4 volte al mese"),
        QuestionOption(4, "2-3 volte a settimana"),
        QuestionOption(5, "4 o più volte a settimana")
    )

    val ALCOHOL_UNITS_OPTIONS = listOf(
        QuestionOption(1, "1-2 unità"),
        QuestionOption(2, "3-4 unità"),
        QuestionOption(3, "5-6 unità"),
        QuestionOption(4, "7-9 unità"),
        QuestionOption(99, "Non applicabile")
    )

    val ACADEMIC_PERFORMANCE_OPTIONS = listOf(
        QuestionOption(0, "Nullo"),
        QuestionOption(1, "Molto scarso"),
        QuestionOption(2, "Scarso"),
        QuestionOption(3, "Buono"),
        QuestionOption(4, "Molto buono"),
        QuestionOption(5, "Ottimo")
    )

    val LIMIT_METHOD_OPTIONS = listOf(
        QuestionOption(1, "Tramite strumenti di \"Limiti ai Tempi di Utilizzo\""),
        QuestionOption(2, "Senza l'uso di strumenti esterni")
    )

    val NOT_INTERESTED_REASON_OPTIONS = listOf(
        QuestionOption(1, "Non credo sarei in grado di farlo"),
        QuestionOption(2, "Non mi interessa ridurre l'uso dei Social Media"),
        QuestionOption(3, "Uso già i Social Media per meno di 30 minuti al giorno"),
        QuestionOption(4, "Altro")
    )

    // Psychometric scale options
    val BSMAS_OPTIONS = listOf(
        QuestionOption(1, "Molto raramente"),
        QuestionOption(2, "Raramente"),
        QuestionOption(3, "Qualche volta"),
        QuestionOption(4, "Spesso"),
        QuestionOption(5, "Molto spesso")
    )

    val PHQ9_GAD7_OPTIONS = listOf(
        QuestionOption(0, "Mai"),
        QuestionOption(1, "Alcuni giorni"),
        QuestionOption(2, "Oltre la metà dei giorni"),
        QuestionOption(3, "Quasi ogni giorno")
    )

    val PHQ9_DIFFICULTY_OPTIONS = listOf(
        QuestionOption(0, "Per niente difficile"),
        QuestionOption(1, "Abbastanza difficile"),
        QuestionOption(2, "Molto difficile"),
        QuestionOption(3, "Estremamente difficile")
    )

    val FOMO_OPTIONS = listOf(
        QuestionOption(1, "Per niente vero"),
        QuestionOption(2, "Poco vero"),
        QuestionOption(3, "Abbastanza vero"),
        QuestionOption(4, "Molto vero"),
        QuestionOption(5, "Estremamente vero")
    )

    val PSS10_OPTIONS = listOf(
        QuestionOption(0, "Mai"),
        QuestionOption(1, "Quasi mai"),
        QuestionOption(2, "A volte"),
        QuestionOption(3, "Abbastanza spesso"),
        QuestionOption(4, "Molto spesso")
    )

    // ========================================================================
    // BASELINE QUESTIONNAIRE (T0)
    // ========================================================================

    fun getBaselineQuestions(): List<QuestionDefinition> {
        val questions = mutableListOf<QuestionDefinition>()

        // Eligibility screening
        questions.add(QuestionDefinition(
            variableName = "is_unito_student",
            label = "Sei uno/a studente/ssa UniTO?",
            type = QuestionType.YESNO,
            options = YESNO_OPTIONS
        ))

        questions.add(QuestionDefinition(
            variableName = "age_group",
            label = "Età",
            type = QuestionType.RADIO,
            options = AGE_GROUP_OPTIONS,
            showIf = BranchingCondition("is_unito_student", BranchingOperator.EQUALS, 1)
        ))

        // Demographics (show if eligible)
        questions.add(QuestionDefinition(
            variableName = "gender",
            label = "Qual è il genere con cui ti identifichi attualmente?",
            type = QuestionType.RADIO,
            options = GENDER_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "gender_other_specify",
            label = "Specifica il genere",
            type = QuestionType.TEXT,
            showIf = BranchingCondition("gender", BranchingOperator.EQUALS, 6),
            required = false
        ))

        questions.add(QuestionDefinition(
            variableName = "nationality",
            label = "Nazionalità",
            type = QuestionType.RADIO,
            options = NATIONALITY_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "field_of_study",
            label = "Qual è il tuo ambito di studi?",
            type = QuestionType.RADIO,
            options = FIELD_OF_STUDY_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "out_of_town",
            label = "Sei uno studente fuorisede?",
            type = QuestionType.RADIO,
            options = OUT_OF_TOWN_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "living_arrangement",
            label = "Attualmente con chi vivi?",
            type = QuestionType.RADIO,
            options = LIVING_ARRANGEMENT_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "family_cohesion",
            label = "Come giudichi il grado di coesione del tuo nucleo familiare?",
            type = QuestionType.RADIO,
            options = FAMILY_COHESION_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "relationship_status",
            label = "Qual è il tuo stato sentimentale?",
            type = QuestionType.RADIO,
            options = RELATIONSHIP_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "sexual_orientation",
            label = "Quale delle seguenti opzioni rappresenta meglio il modo in cui pensi a te stesso?",
            type = QuestionType.RADIO,
            options = SEXUAL_ORIENTATION_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "sexual_orientation_other",
            label = "Specifica il termine che utilizzi",
            type = QuestionType.TEXT,
            showIf = BranchingCondition("sexual_orientation", BranchingOperator.EQUALS, 5),
            required = false
        ))

        // Health questions
        questions.add(QuestionDefinition(
            variableName = "psychiatric_relatives",
            label = "Hai parenti di primo/secondo grado con patologie psichiatriche diagnosticate?",
            type = QuestionType.YESNO,
            options = YESNO_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "psychologist_care",
            label = "Sei attualmente in cura da uno psicologo?",
            type = QuestionType.YESNO,
            options = YESNO_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "psychiatrist_care",
            label = "Sei attualmente in cura da uno psichiatra?",
            type = QuestionType.YESNO,
            options = YESNO_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "psychiatric_diagnosis",
            label = "Hai una diagnosi di patologia psichiatrica?",
            type = QuestionType.YESNO,
            options = YESNO_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "chronic_condition",
            label = "Soffri di una patologia cronica?",
            type = QuestionType.YESNO,
            options = YESNO_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "economic_situation",
            label = "Come giudichi la situazione economica della tua famiglia rispetto alle tue necessità?",
            type = QuestionType.RADIO,
            options = ECONOMIC_SITUATION_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "employment_status",
            label = "Hai un lavoro?",
            type = QuestionType.RADIO,
            options = EMPLOYMENT_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        // Lifestyle
        questions.add(QuestionDefinition(
            variableName = "sport_frequency",
            label = "Pratichi sport?",
            type = QuestionType.RADIO,
            options = SPORT_FREQUENCY_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        // Substance use
        questions.addAll(getSubstanceUseQuestions())

        // Smoking and alcohol
        questions.add(QuestionDefinition(
            variableName = "cigarettes_per_day",
            label = "Quante sigarette fumi al giorno?",
            type = QuestionType.RADIO,
            options = CIGARETTES_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "alcohol_frequency",
            label = "Con quale frequenza assumi sostanze alcoliche?",
            type = QuestionType.RADIO,
            options = ALCOHOL_FREQUENCY_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "alcohol_units",
            label = "Quante unità alcoliche assumi in un giorno tipico di consumo?",
            type = QuestionType.RADIO,
            options = ALCOHOL_UNITS_OPTIONS,
            helperText = "L'unità alcolica corrisponde a 12 grammi di etanolo, contenuti in una lattina di birra (330 ml), un bicchiere di vino (125 ml) o un bicchierino di liquore (40 ml).",
            showIf = BranchingCondition("alcohol_frequency", BranchingOperator.NOT_EQUALS, 1)
        ))

        // Academic performance
        questions.add(QuestionDefinition(
            variableName = "academic_performance_t0",
            label = "Come definiresti il tuo rendimento accademico nell'ultimo mese?",
            type = QuestionType.RADIO,
            options = ACADEMIC_PERFORMANCE_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "other_device_usage_t0",
            label = "Negli ultimi 30 giorni hai utilizzato altri dispositivi oltre lo smartphone (es. PC, tablet...) per navigare sui Social Media?",
            type = QuestionType.YESNO,
            options = YESNO_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        // STL Usage and Group Assignment
        questions.add(QuestionDefinition(
            variableName = "using_time_limits",
            label = "Attualmente stai utilizzando strumenti che limitano il tempo che puoi trascorrere sulle tue app?",
            type = QuestionType.RADIO,
            options = YESNO_OPTIONS,
            showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        questions.add(QuestionDefinition(
            variableName = "time_limits_specify",
            label = "Quale strumento stai utilizzando?",
            type = QuestionType.TEXT,
            showIf = BranchingCondition("using_time_limits", BranchingOperator.EQUALS, 1)
        ))

        questions.add(QuestionDefinition(
            variableName = "interested_in_limit",
            label = "Saresti interessato/a a usare i Social Media per soli 30 minuti al giorno?",
            type = QuestionType.RADIO,
            options = YESNO_OPTIONS,
            showIf = BranchingCondition("using_time_limits", BranchingOperator.EQUALS, 0)
        ))

        questions.add(QuestionDefinition(
            variableName = "limit_method",
            label = "In che modo saresti disposto/a a limitare l'utilizzo dei Social Media a 30 minuti al giorno?",
            type = QuestionType.RADIO,
            options = LIMIT_METHOD_OPTIONS,
            showIf = BranchingCondition("interested_in_limit", BranchingOperator.EQUALS, 1)
        ))

        questions.add(QuestionDefinition(
            variableName = "not_interested_reason",
            label = "Se non sei interessato/a, qual è la motivazione?",
            type = QuestionType.RADIO,
            options = NOT_INTERESTED_REASON_OPTIONS,
            showIf = BranchingCondition("interested_in_limit", BranchingOperator.EQUALS, 0)
        ))

        questions.add(QuestionDefinition(
            variableName = "not_interested_other",
            label = "Specifica la motivazione",
            type = QuestionType.TEXT,
            showIf = BranchingCondition("not_interested_reason", BranchingOperator.EQUALS, 4),
            required = false
        ))

        // Psychometric scales
        questions.addAll(getBSMASQuestions())
        questions.addAll(getPHQ9Questions())
        questions.addAll(getGAD7Questions())
        questions.addAll(getFoMOQuestions())
        questions.addAll(getPSS10Questions())

        return questions
    }

    private fun getSubstanceUseQuestions(): List<QuestionDefinition> {
        val substances = listOf(
            "subst_cocaine" to "Cocaina",
            "subst_hashish" to "Hashish",
            "subst_marijuana" to "Marijuana",
            "subst_synth_cannabinoids" to "Cannabinoidi sintetici (es. Spice, K2)",
            "subst_heroin" to "Eroina",
            "subst_mdma" to "MDMA / metanfetamine / anfetamine",
            "subst_hallucinogens" to "Allucinogeni (LSD, Psilocibina, funghi, Salvia Divinorum)",
            "subst_opioids" to "Farmaci oppiacei senza prescrizione medica",
            "subst_benzodiazepines" to "Benzodiazepine senza prescrizione medica",
            "subst_ketamine" to "Ketamina",
            "subst_methadone" to "Metadone"
        )

        return substances.map { (variable, label) ->
            QuestionDefinition(
                variableName = variable,
                label = label,
                type = QuestionType.SCALE,
                options = SUBSTANCE_USE_OPTIONS,
                showIf = BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99),
                scaleGroup = "substance_use",
                scaleInstructions = "Nell'arco dell'ultimo mese, in media quante volte alla settimana hai usato queste sostanze?"
            )
        }
    }

    private fun getBSMASQuestions(suffix: String = "", isMonthly: Boolean = false): List<QuestionDefinition> {
        val items = listOf(
            "hai trascorso molto tempo pensando ai social media o hai programmato di usarli?",
            "hai sentito il bisogno di usare sempre di più i social media?",
            "hai usato i social media per dimenticare i tuoi problemi personali?",
            "hai provato a smettere di usare i social media senza riuscirci?",
            "sei diventato ansioso o agitato se ti è stato proibito l'uso dei social media?",
            "hai utilizzato i social media così tanto che il loro uso ha avuto un impatto negativo sul tuo lavoro/sui tuoi studi?"
        )

        return items.mapIndexed { index, label ->
            QuestionDefinition(
                variableName = "bsmas_${index + 1}$suffix",
                label = label,
                type = QuestionType.SCALE,
                options = BSMAS_OPTIONS,
                showIf = if (isMonthly) null else BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99),
                scaleGroup = "bsmas$suffix",
                scaleInstructions = "Durante l'ultimo mese con quale frequenza..."
            )
        }
    }

    private fun getPHQ9Questions(suffix: String = "", isMonthly: Boolean = false): List<QuestionDefinition> {
        val items = listOf(
            "Scarso interesse o piacere nel fare le cose",
            "Sentirsi giù, triste o disperato/a",
            "Problemi ad addormentarsi o a dormire tutta la notte senza svegliarsi, o a dormire troppo",
            "Sentirsi stanco/a o avere poca energia",
            "Scarso appetito o mangiare troppo",
            "Avere una scarsa opinione di sé, o sentirsi un/una fallito/a o aver deluso se stesso/a o i propri familiari",
            "Difficoltà a concentrarsi su qualcosa, per esempio leggere il giornale o guardare la televisione",
            "Muoversi o parlare così lentamente da poter essere notato/a da altre persone. O, al contrario, essere così irrequieto/a da muoversi molto più del solito",
            "Pensare che sarebbe meglio morire o farsi del male in un modo o nell'altro"
        )

        val questions = items.mapIndexed { index, label ->
            QuestionDefinition(
                variableName = "phq9_${index + 1}$suffix",
                label = label,
                type = QuestionType.SCALE,
                options = PHQ9_GAD7_OPTIONS,
                showIf = if (isMonthly) null else BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99),
                scaleGroup = "phq9$suffix",
                scaleInstructions = "Nelle ultime 2 settimane, con quale frequenza ti ha dato fastidio ciascuno dei seguenti problemi?"
            )
        }.toMutableList()

        // Add difficulty question
        questions.add(QuestionDefinition(
            variableName = "phq9_difficulty$suffix",
            label = "Quanto questi problemi le hanno reso difficile fare il suo lavoro/occuparsi delle cose/avere buoni rapporti?",
            type = QuestionType.RADIO,
            options = PHQ9_DIFFICULTY_OPTIONS,
            showIf = if (isMonthly) null else BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99)
        ))

        return questions
    }

    private fun getGAD7Questions(suffix: String = "", isMonthly: Boolean = false): List<QuestionDefinition> {
        val items = listOf(
            "Sentirsi nervoso/a, ansioso/a o teso/a",
            "Non riuscire a smettere di preoccuparsi o a tenere sotto controllo le preoccupazioni",
            "Preoccuparsi troppo per varie cose",
            "Avere difficoltà a rilassarsi",
            "Essere talmente irrequieto/a da far fatica a stare seduto/a fermo/a",
            "Infastidirsi o irritarsi facilmente",
            "Avere paura che possa succedere qualcosa di terribile"
        )

        return items.mapIndexed { index, label ->
            QuestionDefinition(
                variableName = "gad7_${index + 1}$suffix",
                label = label,
                type = QuestionType.SCALE,
                options = PHQ9_GAD7_OPTIONS,
                showIf = if (isMonthly) null else BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99),
                scaleGroup = "gad7$suffix",
                scaleInstructions = "Nelle ultime 2 settimane, con quale frequenza ti ha dato fastidio ciascuno dei seguenti problemi?"
            )
        }
    }

    private fun getFoMOQuestions(suffix: String = "", isMonthly: Boolean = false): List<QuestionDefinition> {
        val items = listOf(
            "Ho paura che gli altri facciano esperienze più gratificanti/belle di me",
            "Mi preoccupo quando scopro che i miei amici si stanno divertendo senza di me",
            "Ho paura che i miei amici facciano esperienze più gratificanti/belle di me",
            "Entro in ansia quando non so cosa i miei amici stiano facendo",
            "E' importante per me riuscire a capire su cosa i miei amici stanno scherzando",
            "A volte mi chiedo se non spendo troppo tempo per stare al passo con quanto sta succedendo",
            "Mi infastidisco quando perdo un'opportunità di incontrare i miei amici",
            "Quando faccio qualcosa di bello è importante per me condividere la cosa online (ad es., aggiornando il mio stato, postando foto)",
            "Mi infastidisco quando manco ad un'occasione di gruppo",
            "Quando vado in vacanza, cerco comunque di sapere cosa stanno facendo i miei amici"
        )

        return items.mapIndexed { index, label ->
            QuestionDefinition(
                variableName = "fomo_${index + 1}$suffix",
                label = label,
                type = QuestionType.SCALE,
                options = FOMO_OPTIONS,
                showIf = if (isMonthly) null else BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99),
                scaleGroup = "fomo$suffix",
                scaleInstructions = "Indica quanto ciascuna affermazione è vera per te."
            )
        }
    }

    private fun getPSS10Questions(suffix: String = "", isMonthly: Boolean = false): List<QuestionDefinition> {
        val items = listOf(
            "con che frequenza ti sei sentito fuori di te poiché è avvenuto qualcosa di inaspettato?",
            "con che frequenza hai avuto la sensazione di non essere in grado di avere controllo sulle cose importanti?",
            "con che frequenza ti sei sentito nervoso o 'stressato'?",
            "con che frequenza ti sei sentito fiducioso sulla tua capacità di gestire i tuoi problemi personali?",
            "con che frequenza hai avuto la sensazione che le cose andassero come dicevi tu?",
            "con che frequenza hai avuto la sensazione di non riuscire a star dietro a tutte le cose che dovevi fare?",
            "con che frequenza hai avvertito di essere in grado di controllare ciò che ti irrita nella tua vita?",
            "con che frequenza hai sentito di padroneggiare la situazione?",
            "con che frequenza sei stato arrabbiato per cose che erano fuori dal tuo controllo?",
            "con che frequenza hai avuto la sensazione che le difficoltà si accumulassero a tal punto da non poterle superare?"
        )

        return items.mapIndexed { index, label ->
            QuestionDefinition(
                variableName = "pss10_${index + 1}$suffix",
                label = label,
                type = QuestionType.SCALE,
                options = PSS10_OPTIONS,
                showIf = if (isMonthly) null else BranchingCondition("age_group", BranchingOperator.NOT_EQUALS, 99),
                scaleGroup = "pss10$suffix",
                scaleInstructions = "Nell'ultimo mese..."
            )
        }
    }

    // ========================================================================
    // MONTHLY QUESTIONNAIRE (T1-T4)
    // ========================================================================

    fun getMonthlyQuestions(timepoint: Timepoint, group: Int): List<QuestionDefinition> {
        val questions = mutableListOf<QuestionDefinition>()

        // Academic and device questions
        questions.add(QuestionDefinition(
            variableName = "other_device_usage_m",
            label = "Negli ultimi 30 giorni hai utilizzato altri dispositivi oltre lo smartphone (es. PC, tablet...) per navigare sui Social Media?",
            type = QuestionType.YESNO,
            options = YESNO_OPTIONS
        ))

        questions.add(QuestionDefinition(
            variableName = "academic_performance_m",
            label = "Come definiresti il tuo rendimento accademico nell'ultimo mese?",
            type = QuestionType.RADIO,
            options = ACADEMIC_PERFORMANCE_OPTIONS
        ))

        // Psychometric scales with _m suffix (isMonthly = true to skip eligibility check)
        questions.addAll(getBSMASQuestions("_m", isMonthly = true))
        questions.addAll(getPHQ9Questions("_m", isMonthly = true))
        questions.addAll(getGAD7Questions("_m", isMonthly = true))
        questions.addAll(getFoMOQuestions("_m", isMonthly = true))
        questions.addAll(getPSS10Questions("_m", isMonthly = true))

        // STL Compliance question for Group B (STL Intervention)
        if (group == StudyGroup.STL_INTERVENTION.code) {
            questions.add(QuestionDefinition(
                variableName = "stl_compliance_m",
                label = "I \"Limiti ai Tempi di Utilizzo\" sono attualmente attivi su tutte le app social media?",
                type = QuestionType.YESNO,
                options = YESNO_OPTIONS
            ))
        }

        // Exit question for T4 + Group 2
        if (timepoint == Timepoint.T4 && group == StudyGroup.STL_INTERVENTION.code) {
            questions.add(QuestionDefinition(
                variableName = "continue_stl",
                label = "Continuerai a usare gli strumenti di \"Limiti ai Tempi di Utilizzo\" anche dopo la fine dello studio?",
                type = QuestionType.YESNO,
                options = YESNO_OPTIONS
            ))
        }

        return questions
    }
}
