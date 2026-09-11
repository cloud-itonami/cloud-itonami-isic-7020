(ns consulting.facts
  "Per-jurisdiction management-consultancy professional-standards
  regulatory catalog -- the G2-style spec-basis table the Consulting
  Engagement Governor checks every finding/research proposal against
  ('did the advisor cite an OFFICIAL professional-standards source for
  this jurisdiction, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's recognized management-
  consulting professional body (see `:provenance`); they are a
  STARTING catalog, not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to
  `catalog`, cite a real source, done -- never invent a jurisdiction's
  requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  scope-of-engagement-document/client-data-access-authorization-
  record/methodology-citation-record/conflict-of-interest-disclosure-
  record evidence set submitted in some form; `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:finding/research` proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "一般社団法人日本コンサルティング協会 (JMCA, Japan Management Consultants Association)"
          :legal-basis "経営コンサルタント倫理綱領 (Code of Ethics for Management Consultants)"
          :national-spec "コンサルティング業務の独立性・秘密保持・利益相反開示基準"
          :provenance "https://www.jmca.or.jp/"
          :required-evidence ["契約範囲確認書 (scope-of-engagement-document)"
                              "クライアントデータアクセス許可記録 (client-data-access-authorization-record)"
                              "方法論引用記録 (methodology-citation-record)"
                              "利益相反開示記録 (conflict-of-interest-disclosure-record)"]}
   "USA" {:name "United States"
          :owner-authority "Institute of Management Consultants USA (IMC USA)"
          :legal-basis "IMC USA Code of Ethics / Certified Management Consultant (CMC) standard"
          :national-spec "Independence, confidentiality and conflict-of-interest disclosure requirements"
          :provenance "https://www.imcusa.org/page/CodeofEthics"
          :required-evidence ["Scope-of-engagement document"
                              "Client-data-access-authorization record"
                              "Methodology-citation record"
                              "Conflict-of-interest-disclosure record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Management Consultancies Association (MCA) / Chartered Management Institute (CMI)"
          :legal-basis "MCA Code of Conduct"
          :national-spec "Independence, confidentiality and conflict-of-interest disclosure requirements"
          :provenance "https://www.mca.org.uk/about-us/code-of-conduct"
          :required-evidence ["Scope-of-engagement document"
                              "Client-data-access-authorization record"
                              "Methodology-citation record"
                              "Conflict-of-interest-disclosure record"]}
   "DEU" {:name "Germany"
          :owner-authority "Bundesverband Deutscher Unternehmensberater (BDU)"
          :legal-basis "BDU-Standesregeln (professional code of conduct)"
          :national-spec "Unabhängigkeits-, Vertraulichkeits- und Interessenkonfliktoffenlegungsanforderungen"
          :provenance "https://www.bdu.de/"
          :required-evidence ["Auftragsumfangsdokument (scope-of-engagement-document)"
                              "Kundendatenzugriffsgenehmigung (client-data-access-authorization-record)"
                              "Methodikzitatnachweis (methodology-citation-record)"
                              "Interessenkonfliktoffenlegung (conflict-of-interest-disclosure-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to issue a
  deliverable on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-7020 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `consulting.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
