# Tour 9, lot tests-fragiles : décisions

- TutoKickSoundTest : chaque test coupe le réseau (proxy vers un port fermé) et vérifie qu'aucune adresse n'est demandée ; le test du kick fixe le catalogue des packs. Retour arrière : revert c2549fdb, f597aaa2.
- Tour5BloquantsTest : pas de correction sans cause prouvée ; le message d'échec dit la piste montrée et les notes par piste, pour nommer la cause au prochain rouge. Retour arrière : revert 511b956a.
