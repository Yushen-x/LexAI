# LexAI: Intelligent Legal Assistance System - Project Charter

## 1. Project Overview

**Project Name:** LexAI: Intelligent Legal Assistance System

| Item | Detail |
|------|--------|
| Project Title | LexAI: Intelligent Legal Assistance System |
| Project ID | LEXAI-2026-SPRING |
| Project Type | Course Project Prototype |
| Project Start Date | March 30, 2026 |
| Projected Finish Date | June 14, 2026 |
| Project Manager | Xiangyu Xiao |
| Charter Date | March 29, 2026 |
| Faculty Advisor | Bowen Du |

## 2. Stakeholder Analysis

| Name | Title or Role | Phone Number | E-mail Address | Participation Focus |
|------|---------------|--------------|----------------|---------------------|
| Xiangyu Xiao | Team Leader / Project Manager / Security and Compliance Reviewer | 19107898485 | 2351110@tongji.edu.cn | Participates across architecture design, AI capability integration, frontend and backend development, system integration, testing, and final delivery, while also taking on milestone control and cross-module coordination |
| Baoyi Hu | Team Member | 18702192128 | 2353409@tongji.edu.cn | Participates across platform construction, with stronger focus on model-service refinement, output-logic tuning, and feature-quality improvement |
| Sirui Da | Team Member | 18190637896 | 2352288@tongji.edu.cn | Participates across platform construction, with stronger focus on workflow organization, interaction polishing, and presentation-layer consistency |
| Qi Lin | Team Member | 13560269169 | 2352609@tongji.edu.cn | Participates across platform construction, with stronger focus on interface integration, testing follow-up, deployment preparation, and document consolidation |
| Bowen Du | Faculty Advisor / Course Supervisor | N/A | N/A | Provides academic guidance, milestone review, and course-level feedback |
| TBD | Legal Reviewer | N/A | N/A | Reviews legal logic, disclaimer wording, and the suitability of legal templates |
| External Pilot User Group | Pilot Users | N/A | N/A | Serves as a separate external user group for limited pilot testing and usability feedback |

The four student members are the core platform-construction team. Pilot users, legal reviewers, and course-side reviewers are separate external stakeholders.

## 3. Project Description

### 3.1 Background

LexAI is a project that explores how AI can support legal-service workflows in a controlled prototype setting. The project is built around the current growth in legal-service demand, the uneven distribution of professional support, and the opportunity created by large language models, retrieval-based methods, and structured output design.

This prototype is designed for Chinese civil law scenarios; all legal references, templates, and example cases are based on the PRC Civil Code and related domestic regulations, and are intended for academic demonstration purposes only.

### 3.2 Problem Statement

| Item | Description |
|------|-------------|
| Challenge 1 | Professional legal services remain unevenly distributed across regions and user groups within China. |
| Challenge 2 | Individuals and small organizations often face high consultation and document-preparation costs under the current legal service landscape. |
| Challenge 3 | Legal work includes repetitive retrieval, drafting, and review tasks that are suitable for AI-assisted support, particularly within the context of Chinese civil law. |
| Opportunity | A prototype can be developed to verify the feasibility of AI-assisted legal workflows for Chinese civil law scenarios before any broader deployment is considered. |

### 3.3 Target Outcomes

| Impact Area | Desired Impact |
|-------------|----------------|
| Efficiency | Reduce the time required for representative legal consultation and drafting tasks based on Chinese civil law materials. |
| Quality | Improve the consistency and traceability of prototype-generated outputs within the defined legal scope. |
| Demonstration | Deliver a usable campus-scale prototype covering legal consultation, case analysis, contract review, and selected document-generation workflows, all aligned with Chinese civil law practice. |
| Future Work | Build a reusable technical and evaluation foundation for later iterations, with the understanding that any expansion beyond Chinese civil law would require additional legal validation. |

## 4. Measurable Organizational Value (MOV)

| Objective | Metric | Baseline (March 2026) | Target (By June 2026) |
|-----------|--------|----------------------|----------------------|
| Improve task efficiency | Average completion time for representative tasks | 100% manual baseline | >= 20% reduction in controlled testing |
| Improve output quality | Review checklist pass rate for generated content | 75% | >= 85% |
| Improve contract risk detection | Recall on contract-review benchmark set | 65% | >= 80% |
| Improve accessibility | Number of pilot users completing at least one workflow | 0 | >= 20 users |
| Improve workflow stability | Core workflow success rate during demo and pilot | 85% | >= 95% |
| Reduce hallucination risk | Unsupported claim rate in audited samples | 15% | <= 8% |

## 5. Project Scope

### 5.1 In-Scope Items

| In-Scope Item | Description |
|---------------|-------------|
| Legal consultation | Prototype workflow for common legal questions with structured answers and boundary reminders |
| Case analysis | Basic case-summary and issue-analysis support for selected scenarios |
| Contract review | Risk identification and revision suggestions for sample contract clauses |
| Document generation | Selected legal-document drafting support for limited templates |
| Knowledge support | Basic legal knowledge organization and retrieval support for pilot scenarios |
| System delivery | Internal demo deployment, limited pilot testing, logging, and evaluation records |

### 5.2 Out-of-Scope Items

| Out-of-Scope Item | Description |
|-------------------|-------------|
| Formal legal representation | No court, arbitration, or dispute-handling representation |
| Binding legal advice | System outputs are not official legal opinions without professional review |
| Commercial operation | No production-grade public service or 24/7 commercial deployment |
| Full legal coverage | No attempt to cover all legal domains or international legal systems |
| Full enterprise governance | No enterprise-grade compliance program beyond course-project controls |

### 5.3 Requirement Breakdown Structure (RBS)

The RBS decomposes the stakeholder requirements into progressively detailed levels.

```
RBS: LexAI Intelligent Legal Assistance System
├── R1  Functional Requirements
│   ├── R1.1  Legal Consultation Module
│   │   ├── R1.1.1  Natural-language question input
│   │   ├── R1.1.2  Structured answer generation with legal citations
│   │   └── R1.1.3  Boundary and disclaimer reminders
│   ├── R1.2  Case Analysis Module
│   │   ├── R1.2.1  Case-fact extraction and summarization
│   │   ├── R1.2.2  Legal-issue identification
│   │   └── R1.2.3  Applicable-law matching and suggestion
│   ├── R1.3  Contract Review Module
│   │   ├── R1.3.1  Clause-level risk identification
│   │   ├── R1.3.2  Revision suggestion generation
│   │   └── R1.3.3  Risk-level scoring and report output
│   ├── R1.4  Document Generation Module
│   │   ├── R1.4.1  Template selection and parameter input
│   │   ├── R1.4.2  Draft generation based on user inputs
│   │   └── R1.4.3  Format validation and export
│   └── R1.5  Knowledge Support Module
│       ├── R1.5.1  Legal knowledge base construction
│       ├── R1.5.2  RAG-based retrieval and grounding
│       └── R1.5.3  Knowledge update and maintenance
├── R2  Non-Functional Requirements
│   ├── R2.1  Performance
│   │   ├── R2.1.1  P95 response time ≤ 5 seconds
│   │   └── R2.1.2  System availability ≥ 95% during demo
│   ├── R2.2  Security and Privacy
│   │   ├── R2.2.1  No real personal data in testing
│   │   ├── R2.2.2  Access control for all endpoints
│   │   └── R2.2.3  Compliance with PRC Cybersecurity Law basics
│   ├── R2.3  Usability
│   │   ├── R2.3.1  Intuitive web-based user interface
│   │   ├── R2.3.2  Clear error messages and guidance
│   │   └── R2.3.3  Mobile-responsive layout
│   └── R2.4  Quality and Accuracy
│       ├── R2.4.1  ≥ 85% checklist pass rate for legal accuracy
│       ├── R2.4.2  ≤ 8% hallucination rate in audited samples
│       └── R2.4.3  ≥ 80% recall on contract-review benchmark
└── R3  Delivery Requirements
    ├── R3.1  Deployable prototype for campus-scale demo
    ├── R3.2  Complete project documentation set (charter, design docs, test reports)
    ├── R3.3  Pilot testing with ≥ 20 users
    ├── R3.4  Final presentation and defense materials
    └── R3.5  Project closeout and archival materials
```

### 5.4 Work Breakdown Structure (WBS)

The WBS decomposes the project into three levels of manageable work packages, totaling approximately 65 leaf-level packages.

```
WBS: LexAI Intelligent Legal Assistance System
├── 1.0  Project Management                                    (60 hrs)
│   ├── 1.1  Project planning and charter preparation             (20 hrs)
│   │   ├── 1.1.1  Requirement research and scope definition
│   │   ├── 1.1.2  Charter writing (including RBS / WBS)
│   │   └── 1.1.3  Schedule and resource planning
│   ├── 1.2  Weekly progress tracking and coordination             (22 hrs)
│   │   ├── 1.2.1  Weekly meeting organization and minutes
│   │   └── 1.2.2  Task assignment and progress updates
│   ├── 1.3  Risk monitoring and mitigation                        (10 hrs)
│   │   ├── 1.3.1  Risk register maintenance
│   │   └── 1.3.2  Mitigation action execution and tracking
│   └── 1.4  Stakeholder communication and reporting               ( 8 hrs)
│       ├── 1.4.1  Faculty advisor communication and feedback
│       └── 1.4.2  Phase-end progress reporting
├── 2.0  Requirement Analysis                                    (36 hrs)
│   ├── 2.1  Stakeholder requirement gathering                     (12 hrs)
│   │   ├── 2.1.1  Target user requirement research
│   │   ├── 2.1.2  Legal scenario mapping and classification
│   │   └── 2.1.3  Competitive analysis and reference study
│   ├── 2.2  Scenario definition and prioritization                (10 hrs)
│   │   ├── 2.2.1  Core use-scenario definition
│   │   └── 2.2.2  Scenario priority matrix development
│   ├── 2.3  Evaluation criteria and benchmark design              ( 8 hrs)
│   │   ├── 2.3.1  Functional test case design
│   │   └── 2.3.2  Quality benchmark set construction
│   └── 2.4  Requirement documentation and review                  ( 6 hrs)
│       ├── 2.4.1  Requirement specification writing
│       └── 2.4.2  Requirement review and baseline freeze
├── 3.0  System Design                                            (58 hrs)
│   ├── 3.1  System architecture design                            (16 hrs)
│   │   ├── 3.1.1  Overall technology selection
│   │   ├── 3.1.2  Module partitioning and interface definition
│   │   └── 3.1.3  Data model design
│   ├── 3.2  RAG and retrieval strategy design                     (12 hrs)
│   │   ├── 3.2.1  Knowledge base structure design
│   │   └── 3.2.2  Retrieval and ranking strategy development
│   ├── 3.3  Prompt engineering and output structure design         (10 hrs)
│   │   ├── 3.3.1  Prompt template design
│   │   └── 3.3.2  Output format and constraint rule definition
│   ├── 3.4  UI/UX design and prototyping                          (12 hrs)
│   │   ├── 3.4.1  User interaction flow design
│   │   ├── 3.4.2  Page prototype and visual mockup
│   │   └── 3.4.3  Responsive design plan
│   └── 3.5  Test plan and environment setup                       ( 8 hrs)
│       ├── 3.5.1  Test strategy and case planning
│       └── 3.5.2  Development and test environment configuration
├── 4.0  Development                                              (238 hrs)
│   ├── 4.1  Backend API and service framework                     (40 hrs)
│   │   ├── 4.1.1  Project scaffolding and base configuration
│   │   ├── 4.1.2  Authentication and authorization middleware
│   │   ├── 4.1.3  Core API route implementation
│   │   └── 4.1.4  Logging, monitoring, and error handling
│   ├── 4.2  Legal consultation module                             (30 hrs)
│   │   ├── 4.2.1  Question classification and intent recognition
│   │   ├── 4.2.2  Answer generation pipeline development
│   │   └── 4.2.3  Legal citation and disclaimer integration
│   ├── 4.3  Case analysis module                                  (28 hrs)
│   │   ├── 4.3.1  Case fact structured extraction
│   │   ├── 4.3.2  Issue identification and legal applicability analysis
│   │   └── 4.3.3  Analysis report formatted output
│   ├── 4.4  Contract review module                                (32 hrs)
│   │   ├── 4.4.1  Contract clause parsing and segmentation
│   │   ├── 4.4.2  Risk clause identification engine
│   │   └── 4.4.3  Revision suggestion generation and risk scoring
│   ├── 4.5  Document generation module                            (28 hrs)
│   │   ├── 4.5.1  Template management and parameterized filling
│   │   ├── 4.5.2  Intelligent draft generation
│   │   └── 4.5.3  Format validation and multi-format export
│   ├── 4.6  Knowledge base and RAG integration                    (24 hrs)
│   │   ├── 4.6.1  Legal knowledge data preprocessing and ingestion
│   │   ├── 4.6.2  Vector retrieval service setup
│   │   └── 4.6.3  RAG pipeline end-to-end integration
│   ├── 4.7  Frontend web application                              (36 hrs)
│   │   ├── 4.7.1  Page framework and routing setup
│   │   ├── 4.7.2  Core feature page development
│   │   ├── 4.7.3  Interaction effects and responsive adaptation
│   │   └── 4.7.4  User feedback and error notification components
│   └── 4.8  System integration and API wiring                     (20 hrs)
│       ├── 4.8.1  Frontend-backend integration
│       ├── 4.8.2  Inter-module interface integration
│       └── 4.8.3  End-to-end workflow verification
├── 5.0  Testing and Optimization                                 (60 hrs)
│   ├── 5.1  Unit and integration testing                          (16 hrs)
│   │   ├── 5.1.1  Backend API unit testing
│   │   └── 5.1.2  Inter-module integration testing
│   ├── 5.2  Benchmark evaluation and quality audit                (14 hrs)
│   │   ├── 5.2.1  Legal accuracy benchmark testing
│   │   └── 5.2.2  Hallucination rate audit and output quality evaluation
│   ├── 5.3  Performance testing and optimization                  (10 hrs)
│   │   ├── 5.3.1  Response time and throughput testing
│   │   └── 5.3.2  Performance bottleneck analysis and optimization
│   ├── 5.4  Security and data-handling verification               ( 8 hrs)
│   │   ├── 5.4.1  Data privacy compliance check
│   │   └── 5.4.2  API security testing
│   └── 5.5  Defect fixing and regression testing                  (12 hrs)
│       ├── 5.5.1  Defect classification and priority fixing
│       └── 5.5.2  Regression testing and verification
├── 6.0  Pilot Deployment and Demo                                (40 hrs)
│   ├── 6.1  Demo environment deployment                           (10 hrs)
│   │   ├── 6.1.1  Production environment configuration and deployment
│   │   └── 6.1.2  Data migration and environment verification
│   ├── 6.2  Pilot user testing (≥ 20 users)                       (12 hrs)
│   │   ├── 6.2.1  Pilot user recruitment and training
│   │   └── 6.2.2  Pilot test execution and recording
│   ├── 6.3  Feedback collection and final tuning                  ( 8 hrs)
│   │   ├── 6.3.1  User feedback compilation and analysis
│   │   └── 6.3.2  Critical issue fixes and optimization
│   └── 6.4  Final presentation preparation                        (10 hrs)
│       ├── 6.4.1  Demo script and material creation
│       └── 6.4.2  Rehearsal and final presentation
└── 7.0  Project Closeout                                          (16 hrs)
    ├── 7.1  Final documentation and archiving                      ( 8 hrs)
    │   ├── 7.1.1  Project documentation consolidation
    │   └── 7.1.2  Code and asset archiving
    ├── 7.2  Lessons learned summary                               ( 4 hrs)
    └── 7.3  Project handover and closure                           ( 4 hrs)
```

### 5.5 Total Work Hours Estimation

#### By WBS Category

| WBS Category | Estimated Hours | Percentage |
|--------------|----------------:|-----------:|
| 1.0 Project Management | 60 | 11.8% |
| 2.0 Requirement Analysis | 36 | 7.1% |
| 3.0 System Design | 58 | 11.4% |
| 4.0 Development | 238 | 46.9% |
| 5.0 Testing and Optimization | 60 | 11.8% |
| 6.0 Pilot Deployment and Demo | 40 | 7.9% |
| 7.0 Project Closeout | 16 | 3.1% |
| **Total** | **508** | **100%** |

#### Member × Phase Allocation Matrix (hours)

| Member \ Phase | 1.0 Mgmt | 2.0 Req | 3.0 Design | 4.0 Dev | 5.0 Test | 6.0 Deploy | 7.0 Close | **Total** |
|----------------|--------:|-------:|---------:|-------:|--------:|----------:|---------:|--------:|
| Xiangyu Xiao | 30 | 8 | 16 | 50 | 16 | 12 | 8 | **140** |
| Baoyi Hu | 10 | 10 | 22 | 64 | 14 | 6 | 4 | **130** |
| Sirui Da | 10 | 10 | 12 | 64 | 14 | 10 | 0 | **120** |
| Qi Lin | 10 | 8 | 8 | 60 | 16 | 12 | 4 | **118** |
| **Total** | **60** | **36** | **58** | **238** | **60** | **40** | **16** | **508** |

**Per-member average:** ~127 hours over 11 weeks ≈ ~11.5 hours per person per week.

## 6. Project Schedule Summary

### 6.1 Timeline Overview

| Item | Date |
|------|------|
| Project Start Date | March 30, 2026 |
| Project End Date | June 14, 2026 |
| Total Duration | 11 weeks (77 calendar days) |

### 6.2 Key Milestones

| Phase | Timeline | Deliverables |
|-------|----------|--------------|
| Requirement Analysis & Planning | March 30, 2026 – April 12, 2026 (W1–W2) | Functional specifications, scenario list, evaluation criteria, and project charter |
| System Design | April 13, 2026 – April 26, 2026 (W3–W4) | Architecture design, retrieval strategy, prompt design, UI prototype, and test plan |
| Core Development – Sprint 1 | April 27, 2026 – May 10, 2026 (W5–W6) | Backend framework, knowledge base, legal consultation module |
| Core Development – Sprint 2 | May 11, 2026 – May 24, 2026 (W7–W8) | Case analysis, contract review, document generation, frontend application |
| Integration and Testing | May 25, 2026 – June 7, 2026 (W9–W10) | System integration, benchmark evaluation, performance testing, defect fixes |
| Pilot Deployment and Final Demo | June 8, 2026 – June 14, 2026 (W11) | Pilot testing, feedback collection, final presentation, and project closeout |

### 6.3 Review Plan

| Review Item | Date or Frequency | Purpose |
|-------------|-------------------|---------|
| Weekly Progress Review | Every Monday | Internal progress tracking and issue follow-up |
| Mid-Project Review | May 4, 2026 | Review progress against scope and schedule at the midpoint |
| Pre-Demo Quality Review | June 7, 2026 | Confirm readiness for pilot and final demonstration |
| Final Review | June 14, 2026 | Final presentation and acceptance review |

### 6.4 Gantt Chart

The following Gantt chart shows the planned schedule from March 30 to June 14, 2026 (11 weeks).

```
                           Mar       April                  May                     June
Phase / Work Package       W1   W2   W3   W4   W5   W6   W7   W8   W9   W10  W11
                          3/30 4/06 4/13 4/20 4/27 5/04 5/11 5/18 5/25 6/01 6/08
─────────────────────────────────────────────────────────────────────────────────────
1.0 Project Management    ████ ████ ████ ████ ████ ████ ████ ████ ████ ████ ████
─────────────────────────────────────────────────────────────────────────────────────
2.0 Requirement Analysis   ████ ████
  2.1 Requirement gather   ████ ░░░░
  2.2 Scenario definition  ░░░░ ████
  2.3 Evaluation criteria  ░░░░ ████
  2.4 Req documentation    ░░░░ ████
─────────────────────────────────────────────────────────────────────────────────────
3.0 System Design                    ████ ████
  3.1 Architecture design            ████ ░░░░
  3.2 RAG/retrieval design           ████ ████
  3.3 Prompt/output design           ░░░░ ████
  3.4 UI/UX prototyping              ████ ████
  3.5 Test plan & env setup          ░░░░ ████
─────────────────────────────────────────────────────────────────────────────────────
4.0 Development                                ████ ████ ████ ████
  4.1 Backend API/service                      ████ ████ ░░░░ ░░░░
  4.2 Legal consultation                       ████ ████
  4.3 Case analysis                                  ░░░░ ████ ████
  4.4 Contract review                                ░░░░ ████ ████
  4.5 Document generation                                  ████ ████
  4.6 Knowledge base/RAG                       ████ ████ ░░░░
  4.7 Frontend web app                         ░░░░ ████ ████ ████
  4.8 Integration & wiring                                  ░░░░ ████
─────────────────────────────────────────────────────────────────────────────────────
5.0 Testing & Optimization                                        ████ ████
  5.1 Unit/integration test                                        ████ ░░░░
  5.2 Benchmark & audit                                            ████ ████
  5.3 Performance testing                                          ░░░░ ████
  5.4 Security verification                                        ░░░░ ████
  5.5 Defect fix & retest                                          ████ ████
─────────────────────────────────────────────────────────────────────────────────────
6.0 Pilot & Demo                                                              ████
  6.1 Demo deployment                                                          ████
  6.2 Pilot user testing                                                       ████
  6.3 Feedback & tuning                                                        ████
  6.4 Final presentation                                                       ████
─────────────────────────────────────────────────────────────────────────────────────
7.0 Project Closeout                                                           ████
─────────────────────────────────────────────────────────────────────────────────────

Legend: ████ = primary work period   ░░░░ = supporting/overlapping work

▼ Key Milestones:
  M1  Apr 12 — Requirement baseline frozen
  M2  Apr 26 — System design approved
  M3  May 10 — Sprint 1 complete (core modules v1)
  M4  May 24 — Sprint 2 complete (all modules integrated)
  M5  Jun 07 — Testing complete, demo-ready
  M6  Jun 14 — Final presentation and project closeout
```

## 7. Project Budget Summary

### 7.1 Total Budget

| Budget Item | Estimated Cost (CNY) | Notes |
|-------------|---------------------|-------|
| Total Direct Budget | 3,500 | Modest direct costs only; student labor is not included |

### 7.2 Budget Breakdown

| Phase | Estimated Cost (CNY) | Notes |
|-------|---------------------|-------|
| Requirement Analysis | 300 | Reference materials, scenario collection, and early validation |
| System Design | 400 | Tool setup and environment preparation |
| Development | 1,800 | API usage, implementation support, and iteration expenses |
| Testing and Optimization | 600 | Benchmark runs, defect fixing, and performance checks |
| Pilot Deployment and Final Demo | 400 | Demo environment, printing, and presentation materials |
| **Total** | **3,500** | |

## 8. Quality Issues

### 8.1 Quality Requirements

| Quality Dimension | Target | Validation Method |
|-------------------|--------|-------------------|
| Legal accuracy | >= 85% checklist pass rate | Review checklist scoring on sampled outputs |
| Contract risk detection | >= 80% recall on benchmark set | Benchmark evaluation and reviewer confirmation |
| Hallucination control | <= 8% in audited samples | Audited sample analysis with issue logging |
| Response performance | P95 response time <= 5 seconds in test environment | Performance testing and logging |
| Document format quality | >= 90% template compliance for selected outputs | Template validation and manual inspection |
| Security and privacy | No critical issue in internal testing | Test-data inspection, access review, and checklist verification |

## 9. Resources Required

### 9.1 Human Resources

| Person or Role | Planned Involvement | Notes |
|----------------|---------------------|-------|
| Xiangyu Xiao | Full project participation | Contributes to architecture design, AI capability integration, frontend and backend development, system integration, and overall project coordination |
| Baoyi Hu | Full project participation | Stronger focus on model-service refinement, logic tuning, and platform capability polishing |
| Sirui Da | Full project participation | Stronger focus on workflow organization, interaction presentation, and platform-use coherence |
| Qi Lin | Full project participation | Stronger focus on interface integration, testing follow-up, deployment preparation, and delivery materials |
| Bowen Du | Review as needed | Faculty guidance and milestone feedback |
| Legal Reviewer (TBD) | Review as needed | Legal-logic review for templates and wording |

### 9.2 Technology Stack

| Technology | Purpose |
|------------|---------|
| Tencent AI platform or another approved model service | Model inference and experiment support |
| RAG and knowledge-based tooling | Retrieval support for selected legal scenarios |
| Backend service framework and logging support | API delivery and internal traceability |
| Frontend web application stack | User interaction, result display, and demo presentation |
| Test and demo environment | Internal verification and final presentation |

### 9.3 Facilities

| Facility | Purpose |
|----------|---------|
| Shared development environment | Team collaboration and iterative development |
| Test and demo deployment environment | Verification, rehearsal, and final demonstration |
| Documentation workspace | Requirement notes, review records, and final materials |

### 9.4 Supporting Resources

| Resource | Purpose |
|----------|---------|
| Sample legal documents and templates | Prototype input, drafting support, and review cases |
| Benchmark scenarios and evaluation checklist | Quality measurement and pilot validation |
| Approved sample or anonymized test data | Safe testing and demonstration |
| Demo and presentation materials | Final review and course presentation |

### 9.5 Resource Provision Plan

| Resource | Name of Resource Provider | Date to Be Provided |
|----------|--------------------------|---------------------|
| AI platform access | Project team / approved platform account | March 29, 2026 |
| Test deployment environment | Team environment or campus environment | March 29, 2026 |
| Legal templates and benchmark samples | Team collection with legal-review support | April 5, 2026 |
| Security checklist | Project team and faculty advisor | April 5, 2026 |

## 10. Assumptions and Risks

### 10.1 Assumptions, Constraints, and Dependencies

| Type | Item | Impact on the Project |
|------|------|-----------------------|
| Assumption | All four student members remain available during the semester window | The schedule assumes stable team participation across major phases |
| Assumption | Approved model-platform access is available by the end of the design phase | Development estimates depend on having usable model access in time |
| Assumption | Sample legal templates and evaluation scenarios can be prepared without sensitive real-user data | Testing and demo plans avoid personal or confidential legal data |
| Assumption | Pilot users are available in late May or early June | Pilot validation and feedback collection depend on this participation |
| Constraint | Four-student team size | Limits parallel workload and requires scope discipline |
| Constraint | Fixed project window: March 30 – June 14, 2026 | 11-week window requires a prototype-first delivery strategy |
| Constraint | Course-project positioning | Emphasizes demonstration value over production completeness |
| Constraint | Approved or anonymized data only | Restricts testing materials and demo data sources |
| Dependency | Model-service access and quota | Needed for prototype functions and evaluation runs |
| Dependency | Sample templates and reviewer feedback | Needed for contract review, document generation, and validation |
| Dependency | Test and demo infrastructure | Needed for stable rehearsal and final presentation |
| Dependency | Pilot-user participation | Needed for usability feedback and final verification |

### 10.2 Key Risks and Mitigation

| Risk | Probability | Impact | Mitigation Plan | Owner |
|------|-------------|--------|-----------------|-------|
| Limited manpower and overlapping coursework | High | High | Lock MVP scope, prioritize critical features, and review workload weekly | Xiangyu Xiao |
| AI hallucination in legal answers | Medium | High | Use retrieval grounding, prompt constraints, disclaimer design, and manual benchmark review | Baoyi Hu |
| Data handling mistakes during testing | Low | High | Use anonymized or sample data only, restrict uploads, and maintain a test-data checklist | Xiangyu Xiao |
| Legal interpretation gaps in outputs | Medium | Medium | Limit scenarios and request legal review for templates and wording | Legal Reviewer (TBD) |
| API quota shortages or platform instability | Medium | Medium | Monitor usage, cache results where possible, and prepare fallback demo cases | Qi Lin |

### 10.3 Open Issues

| Open Issues | Current Status |
|-------------|----------------|
| Legal reviewer naming | Still pending confirmation |
| Final pilot scenario set | To be frozen after requirement analysis |
| Ownership after course completion | To be clarified during project closeout |

## 11. Project Administration

### 11.1 Communications Plan

| Audience | Frequency | Channel | Team Contact |
|----------|-----------|---------|--------------|
| Core project team | Weekly | Team meeting and task tracker | Project team |
| Faculty advisor / course supervisor | At key milestones or as needed | Review meeting and summary update | Project team |
| Legal reviewer | At template and evaluation checkpoints | Review session and issue list | Project team |
| Pilot users | During pilot testing | Demo, observation, and feedback form | Project team |

### 11.2 Scope, Quality, and Change Management

| Control Area | Mechanism |
|--------------|-----------|
| Scope baseline | Freeze MVP scope at the end of requirement analysis; new requests must be evaluated for impact on schedule and quality; accept only if core deliverables are not threatened |
| Quality control | Define benchmark scenarios and checklists before development; conduct weekly integration checks during development; close all blocker defects before final demo |
| Change process | Record change request → Assess impact → Review in weekly meeting → Approve/defer/reject → Update tasks and documentation |

### 11.3 Implementation and Closure

| Stage | Timeline | Expected Output |
|-------|----------|-----------------|
| Internal integration and testing | By June 7, 2026 | Stable prototype and test results |
| Pilot deployment and demo preparation | June 8–14, 2026 | Pilot-ready environment and final presentation materials |
| Final closeout | June 14, 2026 | Final documentation, archived materials, and lessons learned |

## 12. Acceptance and Approval

| Acceptance Item | Criteria |
|-----------------|----------|
| Scope completion | All in-scope prototype modules are implemented and demonstrated |
| Quality threshold | Key quality targets are substantially met or any gaps are clearly documented |
| Demo readiness | No blocker defects remain for the final demo and pilot workflow |
| Delivery materials | Pilot checklist and final presentation materials are completed |
| Documentation | Final documentation is submitted and reviewed |

## 13. References

- PRC Civil Code (relevant contract and civil provisions)
- PRC Cybersecurity Law and related data-protection requirements
- Course requirement materials and team planning notes
- Approved legal templates and internal review checklist
- Project README and architecture documentation