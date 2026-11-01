# Understanding the results

maxodiff produces the same results page whether you start it from SAMS, from
the web server, or from the command line. This page explains how to read it.

## The idea in one paragraph

You start with a ranked list of candidate diseases (the *differential
diagnosis*), where rank 1 is the most likely disease. For each diagnostic
procedure, maxodiff simulates what the procedure might find and checks how
much the list would change. **Procedures that are expected to change the list
more are ranked higher**, because they are more likely to help you tell the
candidates apart. See [Background](background.md#how-does-maxodiff-work) for
the method.

## Page overview

From top to bottom, the page shows:

1. **Summary of your input**: the observed and excluded HPO terms, the number
   of diseases, and the number of simulations (repetitions).
2. **Detailed results** for the 10 highest-scoring procedures.
3. **Other tests**: a compact table for all remaining procedures.

If none of the procedures is expected to change the ranking, the page says so
instead of showing results.

## The score (ΔScore)

Every procedure has one number, the **ΔScore**. It is used to order the page:
the procedure with the largest ΔScore comes first.

In each simulation, maxodiff compares the new ranking with the initial
ranking. For every disease it takes the number of rank positions that the
disease moved and divides it by the disease's *initial* rank. The sum over all
diseases is the score of that simulation, and the ΔScore is the average over
all simulations.

Dividing by the initial rank means that a move near the top of the list counts
more than the same move further down. Moving from rank 1 to rank 3 counts as
2, moving from rank 10 to rank 12 counts as 0.2.

!!! note
The ΔScore has no direction. It measures how much the list changes, not
whether it gets better or worse. The direction for each disease is shown by
the bars described below.

The ΔScore is a relative measure for ordering procedures for **one patient**.
Do not compare ΔScore values between different patients or between runs with
different numbers of diseases.

## A detailed result

Each of the top 10 procedures has its own box.

### Header

- The **MAxO term** and its identifier (for example `MAXO:0000973`).
- **ΔScore**, as described above.
- **N Diseases**: the number of diseases that were ranked.
- **N Observed HPO Terms**: the number of HPO terms that this procedure could
  reveal and that are not yet part of the patient's description.

### Average rank change per disease

The bar chart shows one row per disease, in the order of the initial
differential diagnosis (the initial rank is shown before the disease name).

- The **middle line** means "no change".
- A **bar to the left** (blue) means that the disease moves **up** the list on
  average, to a smaller rank number: it becomes more likely.
- A **bar to the right** (red) means that the disease moves **down** the list on
  average: it becomes less likely.
- **One unit is one rank position.** The scale at the top shows the values. The
  numbers are averages over all simulations, so they need not be whole numbers.
  Hover over a bar to see the exact change, for example "Decline by 2.0".

!!! tip "What to look for"
A good procedure moves some candidate diseases clearly up and others
clearly down. If all bars are short, the procedure is not expected to help
distinguish between the candidates.

### Which findings could the procedure reveal?

Under the chart, a table lists the HPO terms that the procedure could
reveal (one column per term).

- **Observed in** shows in how many of the simulations the term was drawn as a
  simulated finding. A darker cell means a more frequent term. Terms that are
  annotated to the highly ranked diseases are drawn more often. Hover over a
  cell to see how frequently the term is annotated to the candidate diseases.
- Each **row** is a disease. The shading of a cell shows how strongly the term
  is associated with that disease, and the number is the information content of
  the most informative common ancestor (see below). An empty cell means that no
  such association was found.

## Other tests

Below the top 10, one table lists all other procedures with:

| Column | Meaning |
|---|---|
| # | Position of the procedure in the overall list |
| MAxO term | Name and identifier |
| ΔScore | The score described above |
| HPO terms | Number of HPO terms that the procedure could reveal |
| Diseases moving up / down | Number of diseases whose average rank gets smaller / larger |

For these procedures, the full details are available in the JSON output.

## Why results can differ slightly between runs

The result of a procedure is simulated by drawing HPO terms at random. Two runs
for the same patient can therefore give slightly different numbers, especially
with few simulations. If the order of the top procedures changes between runs,
increase the number of simulations. Large differences between the top
procedures and the rest are stable.

## Limits of the method

- The results are **estimates**, built on HPO annotations of diseases and MAxO
  annotations of procedures. If an annotation is missing, the procedure's value
  may be underestimated.
- maxodiff does **not** consider cost, risk, invasiveness, or availability of a
  procedure. These are clinical decisions.
- The analysis is limited to the diseases of the differential diagnosis. The
  number of diseases (20 to 40) and the number of simulations (1 to 100) can be
  chosen.