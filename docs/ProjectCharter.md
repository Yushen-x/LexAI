# LexAI: Intelligent Legal Assistance System - Project Charter

## 1. Project Overview

**Project Name:** LexAI: Intelligent Legal Assistance System

| Item | Detail |
|------|--------|
| Project Title | LexAI: Intelligent Legal Assistance System |
| Project ID | LEXAI-2026-SPRING |
| Project Type | Course Project Prototype |
| Project Start Date | March 9, 2026 |
| Projected Finish Date | June 15, 2026 |
| Project Manager | Xiangyu Xiao |
| Charter Date | March 25, 2026 |
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

## 6. Project Schedule Summary

### 6.1 Timeline Overview

| Item | Date |
|------|------|
| Project Start Date | March 9, 2026 |
| Project End Date | June 15, 2026 |

### 6.2 Key Milestones

| Phase | Timeline | Deliverables |
|-------|----------|--------------|
| Requirement Analysis | March 9, 2026 - March 29, 2026 | Functional specifications, scenario list, and evaluation criteria |
| System Design | March 30, 2026 - April 5, 2026 | Architecture design, retrieval strategy, output structure, and test approach |
| Development | April 6, 2026 - May 31, 2026 | Core prototype modules completed and integrated |
| Testing and Optimization | June 1, 2026 - June 7, 2026 | Test report, issue fixes, and quality optimization |
| Pilot Deployment and Final Demo | June 8, 2026 - June 14, 2026 | Pilot-ready prototype, demo materials, and feedback summary |
| Project Closeout | June 15, 2026 | Final documentation, archived materials, and closure summary |

### 6.3 Review Plan

| Review Item | Date or Frequency | Purpose |
|-------------|-------------------|---------|
| Weekly Progress Review | Every Monday | Internal progress tracking and issue follow-up |
| Mid-Project Review | April 27, 2026 | Review progress against scope and schedule |
| Pre-Demo Quality Review | June 7, 2026 | Confirm readiness for pilot and final demonstration |
| Final Review | June 14, 2026 | Final presentation and acceptance review |

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

### 10.1 Assumptions

| Assumption | Planning Implication |
|------------|---------------------|
| All four student members remain available during the semester window | The schedule assumes stable team participation across major phases |
| Approved model-platform access is available by the end of the design phase | Development estimates depend on having usable model access in time |
| Sample legal templates and evaluation scenarios can be prepared without sensitive real-user data | Testing and demo plans avoid personal or confidential legal data |
| Pilot users are available in late May or early June | Pilot validation and feedback collection depend on this participation |

### 10.2 Risk Analysis

| Risk | Probability | Impact | Mitigation Plan | Owner |
|------|-------------|--------|-----------------|-------|
| Limited manpower and overlapping coursework | High | High | Lock MVP scope, prioritize critical features, and review workload weekly | Xiangyu Xiao |
| AI hallucination in legal answers | Medium | High | Use retrieval grounding, prompt constraints, disclaimer design, and manual benchmark review | Baoyi Hu |
| Data handling mistakes during testing | Low | High | Use anonymized or sample data only, restrict uploads, and maintain a test-data checklist | Xiangyu Xiao |
| Legal interpretation gaps in outputs | Medium | Medium | Limit scenarios and request legal review for templates and wording | Legal Reviewer (TBD) |
| API quota shortages or platform instability | Medium | Medium | Monitor usage, cache results where possible, and prepare fallback demo cases | Qi Lin |

### 10.3 Constraints

| Constraint | Impact on the Project |
|------------|----------------------|
| Four-student team size | Limits parallel workload and requires scope discipline |
| Fixed semester deadline of June 15, 2026 | Requires a prototype-first delivery strategy |
| Course-project positioning | Emphasizes demonstration value over production completeness |
| Approved or anonymized data only | Restricts testing materials and demo data sources |

### 10.4 Dependencies

| Dependency | Why It Matters |
|------------|----------------|
| Model-service access and quota | Needed for prototype functions and evaluation runs |
| Sample templates and reviewer feedback | Needed for contract review, document generation, and validation |
| Test and demo infrastructure | Needed for stable rehearsal and final presentation |
| Pilot-user participation | Needed for usability feedback and final verification |

### 10.5 Organizational Impact

| Impact Area | Expected Effect |
|-------------|-----------------|
| Course outcome | Demonstrates a complete AI-assisted legal-workflow prototype in a student-project setting |
| Team capability | Builds experience in LegalTech, AI evaluation, and compliance-aware system design |
| Reusability | Provides reusable architecture ideas, evaluation methods, and demo assets for future work |
| Efficiency | Reduces repetitive drafting and review effort in selected pilot scenarios |

### 10.6 Open Issues

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

### 11.2 Scope Management

| Control Item | Plan |
|--------------|------|
| Scope baseline | Freeze MVP scope at the end of requirement analysis |
| New request review | Evaluate impact on schedule, quality, and demo readiness |
| Scope addition rule | Accept only if the new item does not threaten core deliverables |
| Scope reduction rule | Reduce noncritical items when necessary to protect schedule and quality |

### 11.3 Quality Management

| Activity | Timing | Expected Output |
|----------|--------|-----------------|
| Define benchmark scenarios and checklists | Before development is complete | Stable evaluation baseline |
| Conduct weekly integration checks | During development and testing | Recorded issues and quality follow-up |
| Track and close blocker defects | Before the final demo | Demo-ready prototype |
| Use pilot feedback for final tuning | During the pilot stage | Final optimization summary |

### 11.4 Change Management

| Step | Description |
|------|-------------|
| 1 | Record the requested change and the reason for it |
| 2 | Assess the impact on scope, schedule, quality, and demo readiness |
| 3 | Review the change during the weekly team meeting |
| 4 | Approve, defer, or reject the change |
| 5 | Update tasks and documentation after the decision |

### 11.5 Human Resources

| Member | Shared Participation | Relative Emphasis |
|--------|---------------------|-------------------|
| Xiangyu Xiao | Participates in platform planning, design, implementation, testing, documentation, and presentation | Stronger emphasis on architecture design, AI capability integration, frontend/backend technical implementation, and cross-module integration |
| Baoyi Hu | Participates in platform planning, design, implementation, testing, documentation, and presentation | Stronger emphasis on model-service refinement, logic tuning, and capability polishing |
| Sirui Da | Participates in platform planning, design, implementation, testing, documentation, and presentation | Stronger emphasis on workflow organization, interaction expression, and presentation-layer consistency |
| Qi Lin | Participates in platform planning, design, implementation, testing, documentation, and presentation | Stronger emphasis on interface integration, testing organization, and delivery preparation |

### 11.6 Implementation and Closure

| Stage | Timeline | Expected Output |
|-------|----------|-----------------|
| Internal integration and testing | By June 7, 2026 | Stable prototype and test results |
| Pilot deployment and demo preparation | June 8-14, 2026 | Pilot-ready environment and final presentation materials |
| Final closeout | By June 15, 2026 | Final documentation, archived materials, and lessons learned |

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

## 14. Terminology or Glossary

| Term | Definition |
|------|------------|
| Legal Q&A | AI-assisted workflow for answering selected legal questions |
| Case Analysis | Structured analysis of case facts, issues, and possible legal points |
| RAG | Retrieval-Augmented Generation used to ground model outputs with retrieved knowledge |
| Hallucination | Output that is unsupported, misleading, or factually incorrect |
| MOV | Measurable Organizational Value used to evaluate project success |
| Pilot User | User participating in limited testing of the prototype |

## 15. Appendices (as required)

- System architecture diagrams
- Prompt and retrieval design notes
- Sample legal documents and templates
- Test report and defect summary
- Security and data-handling checklist