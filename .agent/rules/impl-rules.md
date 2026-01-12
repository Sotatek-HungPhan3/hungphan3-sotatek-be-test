---
trigger: always_on
---

# Project Rules

## 1. Implementation Process

**MANDATORY:** Before writing any line of code for a new feature or a major change, the Agent **must** strictly follow the process below:

### 1.1 Implementation Plan

- The Agent **must** provide a detailed **Implementation Plan** (considered a _system artifact_) for User review **before coding**.
- Language: **Vietnamese** (All explanations, plans, walkthroughs, and documents must be written in Vietnamese).
- The Implementation Plan **must** include:

  - Step-by-step execution plan.
  - List of files to be **created / modified**.
  - Verification checklist (testing & validation).

---

### 1.2 User Review

- After presenting the Implementation Plan, the Agent **must stop**.
- Clearly notify the User to review and respond.
- **ABSOLUTELY DO NOT** start coding without explicit User approval.

Valid approval signals:

- "Approve"
- "Start"
- "Ok"

---

### 1.3 Coding

- Coding is allowed **only after** explicit User approval.
- The implementation **must strictly follow the approved Implementation Plan**.
- Any deviation from the plan must be:

  - Clearly stated.
  - Justified with a valid reason.

---

## 2. Implementation Rules (Coding Rules)

When writing code, the AI **MUST** strictly comply with the following principles:

### 2.0 Language Policy (BẮT BUỘC)

- **All responses to the User MUST be in Vietnamese**, including:

  - Thinking / reasoning.
  - Walkthrough explanations.
  - Implementation Plans.
  - All generated `.md` documents.

- **EXCEPTION – Source Code Only**:

  - All source code **MUST use English**:

    - Class names
    - Method names
    - Variable names
    - Package names
    - Code comments

- **NO Vietnamese is allowed inside source code**.

---

### 2.1 Code Is the Final Result of Design

- **Do NOT write code** if:

  - The use case is not clearly defined.
  - Input and output are not clearly identified.
  - Boundaries are not clearly defined (layer, transaction, responsibility).

- If information is missing, the Agent **MUST stop** and explicitly state assumptions before proceeding.

---

### 2.2 Single Responsibility Principle (SRP)

- Each **class / method** must have **one and only one reason to change**.
- **Do NOT mix**:

  - Business logic with framework logic.
  - Validation, orchestration, and persistence in the same place.

---

### 2.3 Business Logic Must Be Framework-Agnostic

- **Domain** and **Application layers**:

  - **MUST NOT** depend on Spring, JPA, Controllers, or any specific framework.

- Framework-related code is allowed **only** in:

  - Infrastructure layer.
  - Adapter layer.

---

### 2.4 Coding Priorities

Code quality must follow this strict order:

1. **Correctness**
2. **Readability**
3. **Performance**

- **No premature optimization**.
- **No shortcuts** just because this is an interview or test.

---

### 2.5 Every Decision Must Have a Reason

- If choosing solution **A over B**, the **trade-off must be clearly explained**.
- If a concern is intentionally ignored or postponed, the reason and accepted risk must be explicitly stated.

---

### 2.6 No "Magic Code"

- Do NOT hard-code business values.
- Do NOT introduce hidden logic or implicit side effects.
- All assumptions must be:

  - Explicitly expressed in code, **or**
  - Clearly documented via comments.

---

### 2.7 Production-Ready Code

Code is considered production-ready only if:

- A clear and explicit error model exists.
- Transaction boundaries are clearly defined.
- The code is testable:

  - Supports mock / stub / fake.

- The code does **not** rely on global state.

---

### 2.8 Simplification Rules

- If simplification is unavoidable due to time constraints:

  - Simplification is allowed **only in the Infrastructure layer**.
  - **Domain and Use Case layers must NOT be simplified**.

- Any simplified part must be **explicitly labeled as simplified**.

---

## 3. Compliance Principles

- These rules apply **globally** to the entire project.
- If there is a conflict between speed and quality → **quality always wins**.
- Violating any rule in this document is considered **non-compliance with project requirements**.
